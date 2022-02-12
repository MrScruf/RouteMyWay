package net.krupizde.RouteMyWay

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.zip.ZipInputStream

@Service
class Gtfs {
    fun load(data: InputStream) {
        val zis = ZipInputStream(data)
        var zipEntry = zis.nextEntry
        var stops: Map<String, Stop> = mapOf();
        var routes: Map<String, Route> = mapOf();
        var trips: Map<String, Trip> = mapOf();
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
        if (stopTimes.isEmpty()) throw IllegalStateException("Stop times are empty")
        val sortedStopTimes = stopTimes.sortedWith(compareBy({ it.tripId }, { it.stopSequence }))
        var prevStopTime = sortedStopTimes[0]
        val tripConnections = mutableListOf<TripConnection>()
        for (stopTime in sortedStopTimes.listIterator(1)) {
            if (prevStopTime.tripId == stopTime.tripId) {
                tripConnections.add(
                    TripConnection(
                        stops.getOrElse(prevStopTime.stopId) { throw IllegalStateException("") },
                        stops.getOrElse(stopTime.stopId) { throw IllegalStateException("") },
                        prevStopTime.departureTime,
                        stopTime.departureTime,
                        trips.getOrElse(stopTime.tripId) { throw IllegalStateException("") }
                    )
                )
            }
            prevStopTime = stopTime
        }
        println(tripConnections)
    }


    private fun parseStopTimes(zis: ZipInputStream): List<StopTime> {
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableListOf<StopTime>()
        for (stopTime in rows) {
            output.add(
                StopTime(
                    stopTime.getValue("trip_id"),
                    stopTime.getValue("arrival_time"),
                    stopTime.getValue("departure_time"),
                    stopTime.getValue("stop_id"),
                    stopTime.getValue("stop_sequence").toInt()
                )
            )
        }
        return output.toList()
    }

    private fun parseTrips(zis: ZipInputStream): Map<String, Trip> {
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableMapOf<String, Trip>()
        for (trip in rows) {
            output[trip.getValue("trip_id")] = Trip(
                trip.getValue("trip_id"),
                trip.getValue("service_id"),
                trip.getValue("route_id"),
                trip.getOrDefault("trip_headsign", ""),
                trip.getOrDefault("trip_short_name", ""),
            )
        }
        return output;
    }

    private fun parseRoutes(zis: ZipInputStream): Map<String, Route> {
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableMapOf<String, Route>()
        for (route in rows) {
            output[route.getValue("route_id")] = Route(
                route.getValue("route_id"),
                route.getOrDefault("route_short_name", ""),
                route.getOrDefault("route_long_name", ""),
                route.getValue("route_type").toInt()
            )
        }
        return output;
    }

    private fun parseStops(zis: ZipInputStream): Map<String, Stop> {
        val reader = zis.bufferedReader(charset = Charsets.UTF_8)
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(reader.readText())
        val output = mutableMapOf<String, Stop>();
        for (stop in rows) {
            output[stop.getValue("stop_id")] = Stop(
                stop.getValue("stop_id"),
                stop.getValue("stop_name"),
                stop.getValue("stop_lat").toDouble(),
                stop.getValue("stop_lon").toDouble()
            )
        }
        return output;
    }
}