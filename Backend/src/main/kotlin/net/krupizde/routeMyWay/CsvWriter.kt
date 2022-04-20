package net.krupizde.routeMyWay

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class CsvWriter {
    fun writeGtfsPathToZip(pathGtfs: PathGtfs): ByteArrayOutputStream {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)
        val stops = ZipEntry("stops.txt")
        val stopsHeader = listOf("stop_id", "stop_name", "stop_lat", "stop_lon")
        val stopsCsv = listOf(stopsHeader, pathGtfs.stops.map { it.toCsv() })
        zos.putNextEntry(stops)
        val stopsTmp = ByteArrayOutputStream()
        csvWriter().writeAll(stopsCsv, stopsTmp)
        zos.write(stopsTmp.toByteArray())
        zos.closeEntry()
        val trips = ZipEntry("trips.txt")
        val tripsHeader = listOf("route_id", "service_id", "trip_id", "trip_headsign", "trip_short_name")
        val tripsCsv = listOf(tripsHeader, pathGtfs.trips.map { it.toCsv() })
        zos.putNextEntry(trips)
        val tripsTmp = ByteArrayOutputStream()
        csvWriter().writeAll(tripsCsv, tripsTmp)
        zos.write(tripsTmp.toByteArray())
        zos.closeEntry()
        val routes = ZipEntry("routes.txt")
        val routesHeader = listOf("route_id", "route_short_name", "route_long_name", "route_type")
        val routesCsv = listOf(routesHeader, pathGtfs.routes.map { it.toCsv() })
        zos.putNextEntry(routes)
        val routesTmp = ByteArrayOutputStream()
        csvWriter().writeAll(routesCsv, routesTmp)
        zos.write(routesTmp.toByteArray())
        zos.closeEntry()
        val stopTimes = ZipEntry("stop_times.txt")
        val stopTimesHeader = listOf("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence")
        val stopTimesCsv = listOf(stopTimesHeader, pathGtfs.stopTimes.map { it.toCsv() })
        zos.putNextEntry(stopTimes)
        val stopTimesTmp = ByteArrayOutputStream()
        csvWriter().writeAll(stopTimesCsv, stopTimesTmp)
        zos.write(stopTimesTmp.toByteArray())
        zos.closeEntry()
        val pathways = ZipEntry("pathways.txt")
        val pathwaysHeader =
            listOf("pathway_id", "from_stop_id", "to_stop_id", "pathway_mode", "is_bidirectional", "traversal_time")
        val pathWaysCsv =
            listOf(pathwaysHeader, pathGtfs.footPaths.mapIndexed { index, footPath -> footPath.toCsv(index) })
        zos.putNextEntry(pathways)
        val pathwaysTmp = ByteArrayOutputStream()
        csvWriter().writeAll(pathWaysCsv, pathwaysTmp)
        zos.write(pathwaysTmp.toByteArray())
        zos.closeEntry()
        zos.close()
        return bos
    }
}