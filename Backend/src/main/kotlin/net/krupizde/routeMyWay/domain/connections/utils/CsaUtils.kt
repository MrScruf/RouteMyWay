package net.krupizde.routeMyWay.domain.connections.utils

import net.krupizde.routeMyWay.domain.connections.obj.BasicStopProfile
import net.krupizde.routeMyWay.domain.connections.obj.BasicStopProfileElement
import net.krupizde.routeMyWay.domain.connections.obj.TripTimeConnectionPair
import java.time.*

class CsaUtils {
    companion object {
        val DEFAULT_BASIC_STOP_PROFILE = BasicStopProfile()
        val DEFAULT_BASIC_STOP_PROFILE_ELEMENT = BasicStopProfileElement()
        val DEFAULT_TRIP_PAIR = TripTimeConnectionPair(Int.MAX_VALUE, null)


        fun intTimeFromInstant(time: Instant): Int {
            val seconds = (time.epochSecond % 86400).toInt()
            val higher = seconds ushr 8 shl 13
            val lower = seconds and 255
            return higher or (1 shl 8) or lower
        }

        fun addTransferToIntTime(time: Int): Int {
            if (time == Int.MAX_VALUE) {
                return Int.MAX_VALUE
            }
            return (time + 256)
        }

        fun addSecondsToIntTime(time: Int, seconds: Int): Int {
            val timeFull = extractSecondsOfDayFromIntTime(time)
            val outTime = timeFull + seconds
            val transfers = time and 7936
            return transfers or (outTime and 255) or (outTime ushr 8 shl 13)
        }

        fun addMinutesToIntTime(time: Int, minutes: Int): Int {
            if (minutes == Int.MAX_VALUE || time == Int.MAX_VALUE) {
                return Int.MAX_VALUE
            }
            return addSecondsToIntTime(time, minutes * 60)
        }

        fun minusSecondsFromIntTime(time: Int, seconds: Int): Int {
            val timeFull = extractSecondsOfDayFromIntTime(time)
            val outTime = timeFull - seconds
            val transfers = time and 7936
            return transfers or (outTime and 255) or (outTime ushr 8 shl 13)
        }

        fun minusMinutesFromIntTimeReprezentation(time: Int, minutes: Int): Int {
            return minusSecondsFromIntTime(time, minutes * 60)
        }

        fun extractSecondsOfDayFromIntTime(time: Int): Int {
            if (time == Int.MAX_VALUE) {
                return time
            }
            val lowerBits = time and 255
            val higherBits = time ushr 13 shl 8
            return higherBits or lowerBits
        }

        fun intTimeMinusIntTimeWithoutTransfers(time1: Int, time2: Int): Int {
            return extractSecondsOfDayFromIntTime(time1) - extractSecondsOfDayFromIntTime(time2)
        }

        fun timeToMinutes(time: Int): Double {
            return (time.toDouble() / 60);
        }

        fun intTimeToInstant(baseDate: Instant, time: Int): Instant {
            val seconds = extractSecondsOfDayFromIntTime(time)
            val startOfTheDay = LocalDate.ofInstant(baseDate, Clock.systemDefaultZone().zone).atStartOfDay()
            val actualTime = startOfTheDay.plusSeconds(seconds.toLong())
            return actualTime.toInstant(ZoneOffset.UTC)
        }
    }
}