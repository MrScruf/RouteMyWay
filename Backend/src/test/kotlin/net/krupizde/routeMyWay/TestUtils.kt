package net.krupizde.routeMyWay

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object TestUtils {
    const val TRAVEL_MODE_WALKING = "WALKING"
    const val TRAVEL_MODE_TRANSIT = "TRANSIT"

    fun timeFromSeconds(seconds: Int): Time{
        val tmpTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())
        return Time(tmpTime.hour, tmpTime.minute, tmpTime.second)
    }
}