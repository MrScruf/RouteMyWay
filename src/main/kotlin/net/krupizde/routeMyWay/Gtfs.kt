package net.krupizde.routeMyWay

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.transaction.Transactional

@Service
class Gtfs(val connectionsService: ConnectionsService, val stopService: StopService, val tripService: TripService) {

    fun loadGtfsData(data: InputStream) {
        val zis = ZipInputStream(data)
        var zipEntry = zis.nextEntry
        var stops: List<Stop> = listOf();
        var routes: List<Route> = listOf();
        var trips: List<Trip> = listOf();
        var stopTimes: List<StopTime> = listOf();
        while (zipEntry != null) {
            println(zipEntry.name)
            when (zipEntry.name) {
                "stops.txt" -> stops = parseStops(zis);
                "routes.txt" -> routes = parseRoutes(zis)
                "stop_times.txt" -> stopTimes = parseStopTimes(zis)
                "trips.txt" -> trips = parseTrips(zis)
            }
            zipEntry = zis.nextEntry;
        }
        zis.close()
        saveToDb(stops, routes, trips, convertGtfsToConnections(stopTimes))

    }

    @Transactional
    protected fun saveToDb(
        stops: List<Stop>,
        routes: List<Route>,
        trips: List<Trip>,
        connections: List<TripConnection>
    ) {
        println("Saving stops")
        stops.forEach { stopService.save(it) }
        println("Saving trips")
        trips.forEach { tripService.save(it) }
        println("Saving connections")
        connections.forEach { connectionsService.save(it) }
        println("Saved")
        println(connectionsService.loadAll())
    }

    private fun convertGtfsToConnections(stopTimes: List<StopTime>): List<TripConnection> {
        if (stopTimes.isEmpty()) throw IllegalStateException("Stop times are empty")
        val sortedStopTimes = stopTimes.sortedWith(compareBy({ it.tripId }, { it.stopSequence }))
        var prevStopTime = sortedStopTimes[0]
        val tripConnections = mutableListOf<TripConnection>()
        for (stopTime in sortedStopTimes.listIterator(1)) {
            if (prevStopTime.tripId == stopTime.tripId) {
                tripConnections.add(
                    TripConnection(
                        prevStopTime.stopId,
                        stopTime.stopId,
                        prevStopTime.departureTime,
                        stopTime.departureTime,
                        stopTime.tripId
                    )
                )
            }
            prevStopTime = stopTime
        }
        return tripConnections
    }

    private fun parseStopTimes(zis: ZipInputStream): List<StopTime> {
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
        return output.toList()
    }

    fun stringToTime(text: String): Time {
        val split = text.split(":")
        return Time(split[0].toInt(), split[1].toInt(), split[2].toInt())
    }

    private fun parseTrips(zis: ZipInputStream): List<Trip> {
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
        return output;
    }

    private fun parseRoutes(zis: ZipInputStream): List<Route> {
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
        return output;
    }

    private fun parseStops(zis: ZipInputStream): List<Stop> {
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<Stop>();
        for (stop in rows) {
            output.add(
                Stop(
                    stop.getValue("stop_id"),
                    stop.getValue("stop_name"),
                    stop.getValue("stop_lat").toDouble(),
                    stop.getValue("stop_lon").toDouble()
                )
            )
        }
        return output;
    }
}