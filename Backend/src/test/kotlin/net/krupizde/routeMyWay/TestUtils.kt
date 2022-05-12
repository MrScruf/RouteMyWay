package net.krupizde.routeMyWay

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

object TestUtils {
    const val TRAVEL_MODE_WALKING = "WALKING"
    const val TRAVEL_MODE_TRANSIT = "TRANSIT"

    fun timeFromSeconds(seconds: Int): Time {
        val tmpTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())
        return Time(tmpTime.hour, tmpTime.minute, tmpTime.second)
    }

    fun compareStops(myConnection: Connection, googleConnection: TransitDetails) =
        abs(myConnection.departureStop.latitude?.minus(googleConnection.departureStop.location.lat) ?: 1.0) < 0.0005 &&
                abs(
                    myConnection.departureStop.longitude?.minus(googleConnection.departureStop.location.lng) ?: 1.0
                ) < 0.0005 &&
                abs(
                    myConnection.arrivalStop.latitude?.minus(googleConnection.arrivalStop.location.lat) ?: 1.0
                ) < 0.0005 &&
                abs(
                    myConnection.arrivalStop.longitude?.minus(googleConnection.arrivalStop.location.lng) ?: 1.0
                ) < 0.0005
}