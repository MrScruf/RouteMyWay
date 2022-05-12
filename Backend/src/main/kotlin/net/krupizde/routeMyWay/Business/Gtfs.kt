package net.krupizde.routeMyWay.business

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.*
import net.krupizde.routeMyWay.*
import net.krupizde.routeMyWay.utils.ThreadSemaphore
import net.krupizde.routeMyWay.utils.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream
import javax.transaction.Transactional
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.streams.asSequence

@Service
class Gtfs(
    private val tripConnectionsService: TripConnectionsService,
    private val stopService: StopService,
    private val tripService: TripService,
    private val footTripConnectionsService: FootConnectionsService,
    private val routeService: RouteService,
    private val locationTypeService: LocationTypeService,
    private val routeTypeService: RouteTypeService,
    private val serviceDayService: ServiceDayService,
    private val dataProvider: DataProvider,
    private val utilService: UtilService,
    private val threadSemaphore: ThreadSemaphore,
    @Value("\${parser.maxDurationFootPathsMinutes:30}") private val maxDurationFootPathsMinutes: Int,
    @Value("\${parser.distanceMultiplicatorConstant:1.5}") private val distanceMultiplicatorConstant: Double,
    @Value("\${parser.generateFootPathsFromStops:false}") private val generateFootPathsFromStops: Boolean,
    @Value("\${parser.saveFullRelationsBetweenTripsAndServices:false}") private val saveFullRelations: Boolean
) {
    private val logger: Logger = LoggerFactory.getLogger(Gtfs::class.java)


    fun loadGtfsData(data: InputStream): Boolean {
        if (!threadSemaphore.value.tryAcquire()) {
            return false
        }
        try {
            load(data)
        } finally {
            threadSemaphore.value.release()
        }
        return true;
    }

    private fun load(data: InputStream) {
        val zis = ZipInputStream(data)
        var zipEntry = zis.nextEntry
        var stops: List<StopGtfs> = listOf();
        var routes: List<RouteGtfs> = listOf();
        var trips: List<TripGtfs> = listOf();
        var pathWays: Set<FootPathGtfs> = setOf();
        var stopTimeGtfs: List<StopTimeGtfs> = listOf();
        var calendarGtfs: List<CalendarGtfs> = listOf()
        var calendarDatesGtfs: List<CalendarDatesGtfs> = listOf()
        logger.info("Starting parsing GTFS Zip file")
        while (zipEntry != null) {
            logger.info("Unzipped file ${zipEntry.name}")
            when (zipEntry.name) {
                "stops.txt" -> stops = parseStops(zis);
                "routes.txt" -> routes = parseRoutes(zis)
                "stop_times.txt" -> stopTimeGtfs = parseStopTimes(zis)
                "trips.txt" -> trips = parseTrips(zis)
                "pathways.txt" -> pathWays = parsePathWays(zis)
                "calendar.txt" -> calendarGtfs = parseCalendars(zis)
                "calendar_dates.txt" -> calendarDatesGtfs = parseCalendarDates(zis)
            }
            zipEntry = zis.nextEntry;
        }
        zis.close()
        val tripConnections = convertStopTimesToConnections(stopTimeGtfs)
        val serviceDays = generateServiceDays(calendarGtfs, calendarDatesGtfs)
        saveAll(stops, routes, trips, pathWays, tripConnections, serviceDays)
        logger.info("Finished parsing and saving data")
        dataProvider.reloadData()
        logger.info("Loaded cache")
    }

    @Transactional
    protected fun saveAll(
        stopsGtfs: List<StopGtfs>, routesGtfs: List<RouteGtfs>, tripsGtfs: List<TripGtfs>,
        pathWaysGtfs: Set<FootPathGtfs>, tripConnectionsGtfs: List<TripConnectionGtfs>, serviceDays: List<ServiceDay>
    ) {
        cleanDb()
        val locationTypesById = createLocationTypes()
        val routeTypesById = createRouteTypes()

        logger.info("Saving stops")
        val stopsGtfsByStopId = stopsGtfs.associateBy { it.stopId }
        val stops = stopService.saveAll(stopsGtfs.mapIndexed { index, it ->
            val wheelChairAccessible =
                if (it.wheelChairBoarding == 0 && it.parentStopId != null) stopsGtfsByStopId[it.parentStopId]?.wheelChairBoarding
                    ?: it.wheelChairBoarding else it.wheelChairBoarding
            it.convertToStop(locationTypesById.getValue(it.locationTypeId ?: 0), wheelChairAccessible, index)
        }).associateBy { it.stopId }

        logger.info("Saving routes")
        val routes = routeService.saveAll(routesGtfs.mapIndexed { index, it ->
            it.convertToRoute(routeTypesById.getValue(it.routeTypeId), index)
        }).associateBy { it.routeId }

        logger.info("Saving service days")
        val serviceDaysSaved = serviceDayService.saveAll(serviceDays)
        val serviceDaysByServiceId = serviceDaysSaved.associateBy { it.serviceId }
        val serviceDaysByServiceIdInt = mutableMapOf<Int, MutableList<Int>>()
        serviceDaysSaved.forEach { serviceDaysByServiceIdInt.getOrPut(it.serviceIdInt) { mutableListOf() }.add(it.id) }
        logger.info("Saving trips")
        val savedTrips = tripService.saveAll(tripsGtfs.mapIndexed { index, it ->
            it.convertToTrip(
                routes.getValue(it.routeId), index, serviceDaysByServiceId.getValue(it.serviceId).serviceIdInt
            )
        })
        if (saveFullRelations) {
            logger.info("Generating relations between trips and serviceDays")
            val relations = savedTrips.flatMap { trip ->
                serviceDaysByServiceIdInt[trip.serviceId]?.map { ServiceDayTripRel(trip.id, it) }?.asIterable()
                    ?: listOf()
            }
            logger.info("Saving relations between trips and serviceDays")
            serviceDayService.saveAllServiceDayTripRel(relations)
        }

        val tripsById = savedTrips.associateBy { it.tripId }
        logger.info("Saving trip connections")
        val tripConnections = tripConnectionsGtfs.mapIndexed { index, it ->
            it.convertToTripConnection(
                stops.getValue(it.departureStopId), stops.getValue(it.arrivalStopId), tripsById.getValue(it.tripId),
                index
            )
        }
        tripConnectionsService.saveAll(tripConnections)

        if (generateFootPathsFromStops) {
            runBlocking { generateAllPathWays(stops.values.toList()) }
        }

        logger.info("Saving foot connections")
        footTripConnectionsService.saveAll(pathWaysGtfs.map {
            it.convertToFootPath(stops.getValue(it.departureStopId), stops.getValue(it.arrivalStopId))
        })
    }

    @Transactional
    protected fun cleanDb() {
        logger.info("Deleting all")
        utilService.truncateConnections()
        utilService.truncateServiceDayTripRel()
        serviceDayService.deleteAll()
        tripService.deleteAll()
        routeService.deleteAll()
        routeTypeService.deleteAll()
        stopService.deleteAll()
        locationTypeService.deleteAll()
        logger.info("Deleted all")
    }

    private fun generateServiceDays(
        calendarGtfs: List<CalendarGtfs>,
        calendarDatesGtfs: List<CalendarDatesGtfs>
    ): List<ServiceDay> {
        val mapCalendarDates = mutableMapOf<String, MutableSet<CalendarDatesGtfs>>()
        val serviceIdsMapping = mutableMapOf<String, Int>()
        calendarDatesGtfs.forEach {
            mapCalendarDates.getOrPut(it.serviceId) { mutableSetOf() }.add(it)
        }
        var serviceDayId = 1
        var serviceId = 1
        val out = calendarGtfs.flatMap { calendar ->
            calendar.startDate.datesUntil(calendar.endDate.plusDays(1)).map { date ->
                val calendarDate = mapCalendarDates[calendar.serviceId]?.find { it.date == date }?.exceptionType
                val willGo =
                    if (calendarDate == null) calendar.daysOfWeek[date.dayOfWeek.value - 1] == 1 else calendarDate == 1
                ServiceDay(
                    calendar.serviceId, serviceIdsMapping.getOrPut(calendar.serviceId) { serviceId++ }, date, willGo,
                    serviceDayId++
                )
            }.asSequence().toList()
        }
        //TODO upravit
        return out;
    }

    private suspend fun generateAllPathWays(stops: List<Stop>) {
        logger.info("Starting generating pathways between stops")
        val index = AtomicInteger();
        coroutineScope {
            for (i in stops.indices) {
                logger.debug("Started generating pathways from stop $i")
                launch(Dispatchers.Default) {
                    val outPathWays = mutableListOf<FootPath>()
                    for (y in i until stops.size) {
                        var distanceInKm = distanceInKm(stops[i], stops[y])
                        distanceInKm *= distanceMultiplicatorConstant
                        val duration =
                            if (stops[i].stopId == stops[y].stopId) 0 else ceil((distanceInKm / 5) * 60).toInt()
                        if (duration > maxDurationFootPathsMinutes || distanceInKm < 0) {
                            continue
                        }
                        outPathWays.add(FootPath(stops[i].id, stops[y].id, duration))
                    }
                    logger.debug("Finished generating from $i. Generated ${outPathWays.size} footConnections. Saving to DB")
                    withContext(Dispatchers.IO) {
                        footTripConnectionsService.saveAll(outPathWays)
                        logger.debug("Finished saving from $i")
                    }
                    index.addAndGet(outPathWays.size)
                }
            }
        }
        logger.info("Generated $index pathways")
        logger.info("Finished generating pathways between stops")
    }

    fun distanceInKm(stop1: Stop, stop2: Stop): Double {
        if (stop1.longitude == null || stop2.longitude == null || stop1.latitude == null || stop2.latitude == null) {
            return -1.0
        }
        val theta: Double = stop1.longitude - stop2.longitude
        var dist =
            sin(deg2rad(stop1.latitude)) * sin(deg2rad(stop2.latitude)) + cos(deg2rad(stop1.latitude)) * cos(
                deg2rad(
                    stop2.latitude
                )
            ) * cos(deg2rad(theta))
        dist = acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515 * 1.609344
        return dist;
    }

    private fun deg2rad(deg: Double): Double = deg * Math.PI / 180.0

    private fun rad2deg(rad: Double): Double = rad * 180.0 / Math.PI

    private fun createLocationTypes(): Map<Int, LocationType> {
        logger.info("Saving location types")
        locationTypeService.save(LocationType(0, "Stop/Platform"))
        locationTypeService.save(LocationType(1, "Station"))
        locationTypeService.save(LocationType(2, "Entrance/Exit"))
        locationTypeService.save(LocationType(3, "Generic Node"))
        locationTypeService.save(LocationType(4, "Boarding Area"))
        return locationTypeService.findAll().associateBy { it.locationTypeId }
    }

    private fun createRouteTypes(): Map<Int, RouteType> {
        logger.info("Saving route types")
        routeTypeService.save(RouteType(0, "Tram, Streetcar, Light rail"))
        routeTypeService.save(RouteType(1, "Subway, Metro"))
        routeTypeService.save(RouteType(2, "Rail"))
        routeTypeService.save(RouteType(3, "Bus"))
        routeTypeService.save(RouteType(4, "Ferry"))
        routeTypeService.save(RouteType(5, "Cable tram"))
        routeTypeService.save(RouteType(6, "Aerial lift"))
        routeTypeService.save(RouteType(7, "Funicular"))
        routeTypeService.save(RouteType(11, "Trolleybus"))
        routeTypeService.save(RouteType(12, "Monorail"))
        return routeTypeService.findAll().associateBy { it.routeTypeId }
    }

    private fun convertStopTimesToConnections(stopTimeGtfs: List<StopTimeGtfs>): List<TripConnectionGtfs> {
        logger.info("Converting GTFS stop times to connections")
        if (stopTimeGtfs.isEmpty()) {
            throw IllegalStateException("Stop times are empty")
        }
        val sortedStopTimes = stopTimeGtfs.sortedWith(compareBy({ it.tripId }, { it.stopSequence }))
        var prevStopTime = sortedStopTimes[0]
        val tripConnections = mutableListOf<TripConnectionGtfs>()
        for (stopTime in sortedStopTimes.listIterator(1)) {
            if (prevStopTime.tripId == stopTime.tripId) {
                tripConnections.add(
                    TripConnectionGtfs(
                        prevStopTime.stopId, stopTime.stopId, prevStopTime.departureTime, stopTime.arrivalTime,
                        stopTime.tripId
                    )
                )
            }
            prevStopTime = stopTime
        }
        logger.info("Finished converting GTFS stop times to connections")
        return tripConnections
    }

    private fun parseStopTimes(zis: ZipInputStream): List<StopTimeGtfs> {
        logger.info("Parsing GTFS stop_times.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<StopTimeGtfs>()
        for (stopTime in rows) {
            output.add(
                StopTimeGtfs(
                    stopTime.getValue("trip_id"),
                    Utils.stringToUintTimeReprezentation(stopTime.getValue("arrival_time")),
                    Utils.stringToUintTimeReprezentation(stopTime.getValue("departure_time")),
                    stopTime.getValue("stop_id"),
                    stopTime.getValue("stop_sequence").toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS stop_times.txt")
        return output.toList()
    }

    private fun parseTrips(zis: ZipInputStream): List<TripGtfs> {
        logger.info("Parsing GTFS trips.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<TripGtfs>()
        for (trip in rows) {
            output.add(
                TripGtfs(
                    trip.getValue("trip_id"),
                    trip.getValue("service_id"),
                    trip.getValue("route_id"),
                    trip.getOrDefault("trip_headsign", ""),
                    trip.getOrDefault("trip_short_name", ""),
                    trip["wheelchair_accessible"]?.toInt(),
                    trip["bikes_allowed"]?.toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS trips.txt")
        return output;
    }

    private fun parseRoutes(zis: ZipInputStream): List<RouteGtfs> {
        logger.info("Parsing GTFS routes.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<RouteGtfs>()
        for (route in rows) {
            output.add(
                RouteGtfs(
                    route.getValue("route_id"),
                    route.getOrDefault("route_short_name", ""),
                    route.getOrDefault("route_long_name", ""),
                    route.getValue("route_type").toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS routes.txt")
        return output;
    }

    private fun parseStops(zis: ZipInputStream): List<StopGtfs> {
        logger.info("Parsing GTFS stops.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = rows.map { stop ->
            StopGtfs(
                stop.getValue("stop_id"), stop["stop_name"], stop["parent_station"], stop["stop_lat"]?.toDouble(),
                stop["stop_lon"]?.toDouble(), stop["location_type"]?.toInt(), stop["wheelchair_boarding"]?.toInt()
            )
        }
        logger.info("Finished parsing GTFS stops.txt")
        return output;
    }

    private fun parseCalendarDates(zis: ZipInputStream): List<CalendarDatesGtfs> {
        logger.info("Parsing GTFS calendar_dates.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = rows.map { calendarDate ->
            CalendarDatesGtfs(
                calendarDate.getValue("service_id"),
                parseDate(calendarDate.getValue("date")),
                calendarDate.getValue("exception_type").toInt()
            )

        }
        logger.info("Finished parsing GTFS calendar_dates.txt")
        return output;
    }

    private fun parseCalendars(zis: ZipInputStream): List<CalendarGtfs> {
        logger.info("Parsing GTFS calendar.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<CalendarGtfs>();
        for (calendar in rows) {
            output.add(
                CalendarGtfs(
                    calendar.getValue("service_id"),
                    parseDate(calendar.getValue("start_date")),
                    parseDate(calendar.getValue("end_date")),
                    listOf(
                        calendar.getValue("monday").toInt(),
                        calendar.getValue("tuesday").toInt(),
                        calendar.getValue("wednesday").toInt(),
                        calendar.getValue("thursday").toInt(),
                        calendar.getValue("friday").toInt(),
                        calendar.getValue("saturday").toInt(),
                        calendar.getValue("sunday").toInt()
                    )
                )
            )
        }
        logger.info("Finished parsing GTFS calendar.txt")
        return output;
    }

    fun parseDate(date: String): LocalDate {
        val year = date.substring(0, 4).toInt()
        val month = date.substring(4, 6).toInt()
        val day = date.substring(6, 8).toInt()
        return LocalDate.of(year, month, day)
    }

    private fun parsePathWays(zis: ZipInputStream): Set<FootPathGtfs> {
        logger.info("Parsing GTFS pathways.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableSetOf<FootPathGtfs>();
        for (pathWay in rows) {
            val time = pathWay.getValue("traversal_time")
            if (time.isBlank()) {
                continue
            }
            output.add(
                FootPathGtfs(
                    pathWay.getValue("from_stop_id"),
                    pathWay.getValue("to_stop_id"),
                    time.toInt()
                )
            )
            if (pathWay.getValue("is_bidirectional") == "1") {
                output.add(
                    FootPathGtfs(
                        pathWay.getValue("to_stop_id"),
                        pathWay.getValue("from_stop_id"),
                        time.toInt()
                    )
                )
            }
        }
        logger.info("Finished parsing GTFS pathways.txt")
        return output;
    }
}