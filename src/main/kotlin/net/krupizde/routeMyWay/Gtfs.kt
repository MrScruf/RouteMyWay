package net.krupizde.routeMyWay

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
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
    private val locationTypeService: LocationTypeService,
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
        var pathWays: Set<FootConnection> = setOf();
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
            runBlocking { generateAllPathWays(stops, pathWays) }
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

    private suspend fun generateAllPathWays(stops: List<Stop>, pathWays: Set<FootConnection>) {
        logger.info("Starting generating pathways between stops")
        val stopsFiltered = stops.filter { it.locationTypeId != 1 }
        coroutineScope {
            for (i in stopsFiltered.indices) {
                logger.info("Started generating from stop $i")
                launch(Dispatchers.Default) {
                    val outPathWays = mutableListOf<FootConnection>()
                    for (y in i until stopsFiltered.size) {
                        var distance = distanceInKm(stopsFiltered[i], stopsFiltered[y])
                        if (distance < 0) continue
                        distance *= (1 + ((distance / 0.5) * 0.2))
                        val duration =
                            if (stopsFiltered[i].stopId == stopsFiltered[y].stopId) 0 else ceil(distance / 5).toInt()
                        if (duration > maxDurationFootPathsMinutes) continue
                        outPathWays.add(FootConnection(stopsFiltered[i].stopId, stopsFiltered[y].stopId, duration))
                    }
                    logger.info("Finished generating from $i. Generated ${outPathWays.size} footConnections.Saving to DB")
                    withContext(Dispatchers.IO) {
                        footTripConnectionsService.saveAll(outPathWays)
                        logger.info("Finished saving from $i")
                    }
                }
            }
        }
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

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    @Transactional
    protected fun saveToDb(
        stops: List<Stop>,
        routes: List<Route>,
        trips: List<Trip>,
        connections: List<TripConnection>
    ) {
        logger.info("Saving data to database")
        createLocationTypes();
        logger.info("Saving stops")
        stopService.saveAll(stops)
        logger.info("Saving trips")
        tripService.saveAll(trips)
        logger.info("Saving connections")
        runBlocking {
            connections.forEach {
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
                        stopTime.departureTime,
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

    fun stringToTime(text: String): Time {
        val split = text.split(":")
        return Time(split[0].toInt(), split[1].toInt(), split[2].toInt())
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
                    stop["location_type"]?.toInt()
                )
            )
        }
        logger.info("Finished parsing GTFS stops.txt")
        return output;
    }

    private fun parsePathWays(zis: ZipInputStream): Set<FootConnection> {
        logger.info("Parsing GTFS pathways.txt")
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableSetOf<FootConnection>();
        for (pathWay in rows) {
            val time = pathWay.getValue("traversal_time")
            if (time.isBlank()) continue
            output.add(
                FootConnection(
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