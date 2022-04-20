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
        writeStops(pathGtfs, zos)
        writeTrips(pathGtfs, zos)
        writeRoutes(pathGtfs, zos)
        writeStopTimes(pathGtfs, zos)
        writePathways(pathGtfs, zos)
        zos.close()
        return bos
    }

    private fun writeStops(pathGtfs: PathGtfs, zos: ZipOutputStream) {
        val stops = ZipEntry("stops.txt")
        val stopsHeader = listOf("stop_id", "stop_name", "stop_lat", "stop_lon")
        val stopsCsv = pathGtfs.stops.map { it.toCsvList() }
        zos.putNextEntry(stops)
        val stopsTmp = ByteArrayOutputStream()
        csvWriter().open(stopsTmp) {
            writeRow(stopsHeader)
            writeRows(stopsCsv)
        }
        zos.write(stopsTmp.toByteArray())
        zos.closeEntry()
    }

    private fun writeTrips(pathGtfs: PathGtfs, zos: ZipOutputStream) {
        val trips = ZipEntry("trips.txt")
        val tripsHeader = listOf("route_id", "service_id", "trip_id", "trip_headsign", "trip_short_name")
        val tripsCsv = pathGtfs.trips.map { it.toCsvList() }
        zos.putNextEntry(trips)
        val tripsTmp = ByteArrayOutputStream()
        csvWriter().open(tripsTmp) {
            writeRow(tripsHeader)
            writeRows(tripsCsv)
        }
        zos.write(tripsTmp.toByteArray())
        zos.closeEntry()
    }

    private fun writeRoutes(pathGtfs: PathGtfs, zos: ZipOutputStream) {
        val routes = ZipEntry("routes.txt")
        val routesHeader = listOf("route_id", "route_short_name", "route_long_name", "route_type")
        val routesCsv = pathGtfs.routes.map { it.toCsvList() }
        zos.putNextEntry(routes)
        val routesTmp = ByteArrayOutputStream()
        csvWriter().open(routesTmp) {
            writeRow(routesHeader)
            writeRows(routesCsv)
        }
        zos.write(routesTmp.toByteArray())
        zos.closeEntry()
    }

    private fun writeStopTimes(pathGtfs: PathGtfs, zos: ZipOutputStream) {
        val stopTimes = ZipEntry("stop_times.txt")
        val stopTimesHeader = listOf("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence")
        val stopTimesCsv = pathGtfs.stopTimes.map { it.toCsvList() }
        zos.putNextEntry(stopTimes)
        val stopTimesTmp = ByteArrayOutputStream()
        csvWriter().open(stopTimesTmp) {
            writeRow(stopTimesHeader)
            writeRows(stopTimesCsv)
        }
        zos.write(stopTimesTmp.toByteArray())
        zos.closeEntry()
    }

    private fun writePathways(pathGtfs: PathGtfs, zos: ZipOutputStream) {
        val pathways = ZipEntry("pathways.txt")
        val pathwaysHeader =
            listOf("pathway_id", "from_stop_id", "to_stop_id", "pathway_mode", "is_bidirectional", "traversal_time")
        val pathWaysCsv = pathGtfs.footPaths.mapIndexed { index, footPath -> footPath.toCsvList(index) }
        zos.putNextEntry(pathways)
        val pathwaysTmp = ByteArrayOutputStream()
        csvWriter().open(pathwaysTmp) {
            writeRow(pathwaysHeader)
            writeRows(pathWaysCsv)
        }
        zos.write(pathwaysTmp.toByteArray())
        zos.closeEntry()
    }
}