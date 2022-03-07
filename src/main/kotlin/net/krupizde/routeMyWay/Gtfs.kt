package net.krupizde.routeMyWay

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream
import javax.transaction.Transactional
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@Service
class Gtfs(
    private val tripConnectionsService: TripConnectionsService,
    private val stopService: StopService,
    private val tripService: TripService,
    private val footTripConnectionsService: FootConnectionsService,
    private val routeService: RouteService,
    private val locationTypeService: LocationTypeService,
    private val routeTypeService: RouteTypeService,
    private val dataCache: DataCache,
    @Value("\${parser.maxDurationFootPathsMinutes:30}") private val maxDurationFootPathsMinutes: Int,
    @Value("\${parser.generateFootPathsFromStops:false}") private val generateFootPathsFromStops: Boolean
) {
    private val logger: Logger = LoggerFactory.getLogger(Gtfs::class.java)

    fun loadGtfsData(data: InputStream) {
        cleanDb()
        val zis = ZipInputStream(data)
        var zipEntry = zis.nextEntry
        var stops: List<Stop> = listOf();
        var routes: List<Route> = listOf();
        var trips: List<Trip> = listOf();
        var pathWays: Set<FootPath> = setOf();
        var stopTimes: List<StopTime> = listOf();
        logger.info("Starting parsing GTFS Zip file")
        while (zipEntry != null) {
            logger.info("Unzipped file ${zipEntry.name}")
            when (zipEntry.name) {
                "stops.txt" -> stops = parseStops(zis);
                "routes.txt" -> routes = parseRoutes(zis)
                "stop_times.txt" -> stopTimes = parseStopTimes(zis)
                "trips.txt" -> trips = parseTrips(zis)
                "pathways.txt" -> pathWays = parsePathWays(zis)
            }
            zipEntry = zis.nextEntry;
        }
        zis.close()
        saveToDb(stops, routes, trips, convertGtfsToConnections(stopTimes))
        if (generateFootPathsFromStops)
            runBlocking { generateAllPathWays(stops) }
        logger.info("Saving foot connections")
        footTripConnectionsService.saveAll(pathWays.toList())
        logger.info("Finished parsing and saving data")
        dataCache.reloadData()
        logger.info("Loaded cache")
    }

    @Transactional
    protected fun cleanDb() {
        logger.info("Deleting trip connections")
        tripConnectionsService.deleteAll()
        logger.info("Deleting foot connections")
        footTripConnectionsService.deleteAll()
        logger.info("Deleting stops")
        stopService.deleteAll()
        logger.info("Deleting trips")
        tripService.deleteAll()
        logger.info("Deleting location types")
        locationTypeService.deleteAll()
    }

    //TODO - generation of footConnections - optimize, better algorithm
    private suspend fun generateAllPathWays(stops: List<Stop>) {
        logger.info("Starting generating pathways between stops")
        val stopsFiltered = stops.filter { it.locationTypeId != 1 }
        val index = AtomicInteger();
        coroutineScope {
            for (i in stopsFiltered.indices) {
                logger.info("Started generating from stop $i")
                launch(Dispatchers.Default) {
                    val outPathWays = mutableListOf<FootPath>()
                    for (y in i until stopsFiltered.size) {
                        var distance = distanceInKm(stopsFiltered[i], stopsFiltered[y])
                        if (distance < 0) continue
                        distance *= (1.2 + ((distance / 0.001) * 0.006))
                        val duration =
                            if (stopsFiltered[i].stopId == stopsFiltered[y].stopId) 0 else ceil(distance / 5).toInt()
                        if (duration > maxDurationFootPathsMinutes) continue
                        outPathWays.add(FootPath(stopsFiltered[i].stopId, stopsFiltered[y].stopId, duration))
                    }
                    logger.info("Finished generating from $i. Generated ${outPathWays.size} footConnections.Saving to DB")
                    withContext(Dispatchers.IO) {
                        footTripConnectionsService.saveAll(outPathWays)
                        logger.info("Finished saving from $i")
                    }
                    index.addAndGet(outPathWays.size)
                }
            }
        }
        logger.info("Generated $index pathways")
        logger.info("Finished generating pathways between stops")
    }

    fun distanceInKm(stop1: Stop, stop2: Stop): Double {
        if (stop1.longitude == null || stop2.longitude == null || stop1.latitude == null || stop2.latitude == null) return -1.0
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

    @Transactional
    protected fun saveToDb(
        stops: List<Stop>,
        routes: List<Route>,
        trips: List<Trip>,
        connections: List<TripConnection>
    ) {
        logger.info("Saving data to database")
        createLocationTypes();
        createRouteTypes();
        logger.info("Saving stops")
        stopService.saveAll(stops)
        logger.info("Saving routes")
        routeService.saveAll(routes)
        logger.info("Saving trips")
        tripService.saveAll(trips)
        logger.info("Saving connections")
        //Multithreaded saving to database
        runBlocking {
            //Before saving, sort connections by departure time, so it doesn't need to be sorted afterwards
            connections.sortedBy { it.departureTime }.forEach {
                launch(Dispatchers.IO) {
                    tripConnectionsService.save(it)
                }
            }
        }
        logger.info("Saved")
    }

    private fun createLocationTypes() {
        logger.info("Saving location types")
        locationTypeService.save(LocationType(0, "Stop/Platform"))
        locationTypeService.save(LocationType(1, "Station"))
        locationTypeService.save(LocationType(2, "Entrance/Exit"))
        locationTypeService.save(LocationType(3, "Generic Node"))
        locationTypeService.save(LocationType(4, "Boarding Area"))
    }

    private fun createRouteTypes() {
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
    }

    //TODO - add data to connection to allow reconstruction back to StopTime
    private fun convertGtfsToConnections(stopTimes: List<StopTime>): List<TripConnection> {
        logger.info("Converting GTFS stop times to connections")
        if (stopTimes.isEmpty()) throw IllegalStateException("Stop times are empty")
        val sortedStopTimes = stopTimes.sortedWith(compareBy({ it.tripId }, { it.stopSequence }))
        var prevStopTime = sortedStopTimes[0]
        val tripConnections = mutableListOf<TripConnection>()
        var i = 1;
        for (stopTime in sortedStopTimes.listIterator(1)) {
            if (prevStopTime.tripId == stopTime.tripId) {
                tripConnections.add(
                    TripConnection(
                        prevStopTime.stopId,
                        stopTime.stopId,
                        prevStopTime.departureTime,
                        stopTime.arrivalTime,
                        stopTime.tripId,
                        i
                    )
                )
                i++;
            }
            prevStopTime = stopTime
        }
        logger.info("Finished converting GTFS stop times to connections")
        return tripConnections
    }

    private fun parseStopTimes(zis: ZipInputStream): List<StopTime> {
        logger.info("Parsing GTFS stop_times.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<StopTime>()
        for (stopTime in rows) {
            output.add(
                StopTime(
                    stopTime.getValue("trip_id"),
                    stringToTime(stopTime.getValue("arrival_time")),
                    stringToTime(stopTime.getValue("departure_time")),
                    stopTime.getValue("stop_id"),
                    stopTime.getValue("stop_sequence").toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS stop_times.txt")
        return output.toList()
    }

    fun stringToTime(text: String): Int {
        val split = text.split(":")
        return Utils.generateTime(split[0].toInt(), split[1].toInt(), split[2].toInt())
    }

    private fun parseTrips(zis: ZipInputStream): List<Trip> {
        logger.info("Parsing GTFS trips.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<Trip>()
        for (trip in rows) {
            output.add(
                Trip(
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

    private fun parseRoutes(zis: ZipInputStream): List<Route> {
        logger.info("Parsing GTFS routes.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<Route>()
        for (route in rows) {
            output.add(
                Route(
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

    private fun parseStops(zis: ZipInputStream): List<Stop> {
        logger.info("Parsing GTFS stops.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<Stop>();
        for (stop in rows) {
            output.add(
                Stop(
                    stop.getValue("stop_id"),
                    stop["stop_name"],
                    stop["stop_lat"]?.toDouble(),
                    stop["stop_lon"]?.toDouble(),
                    stop["location_type"]?.toInt(),
                    stop["wheelchair_boarding"]?.toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS stops.txt")
        return output;
    }

    //TODO - vyresit bi-directional, ukladat je dvakrat
    private fun parsePathWays(zis: ZipInputStream): Set<FootPath> {
        logger.info("Parsing GTFS pathways.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableSetOf<FootPath>();
        for (pathWay in rows) {
            val time = pathWay.getValue("traversal_time")
            if (time.isBlank()) continue
            output.add(
                FootPath(
                    pathWay.getValue("from_stop_id"),
                    pathWay.getValue("to_stop_id"),
                    time.toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS pathways.txt")
        return output;
    }
}