package net.krupizde.routeMyWay.utils

import net.krupizde.routeMyWay.Time
import java.time.LocalTime
import kotlin.math.floor

class Utils {
    companion object {
        fun generateUintTimeReprezentation(hours: Int, minutes: Int, seconds: Int): UInt {
            val time = (hours.toUInt() * 3600u + minutes.toUInt() * 60u + seconds.toUInt())
            val higher = time shr 8 shl 13
            val lower = time and 255u
            return higher or (1u shl 8) or lower
        }

        fun generateUintTimeReprezentation(time: LocalTime): UInt {
            return generateUintTimeReprezentation(time.hour, time.minute, time.second)
        }

        fun generateUintTimeReprezentation(time: Time): UInt {
            return generateUintTimeReprezentation(time.hours, time.minutes, time.seconds)
        }

        fun stringToUintTimeReprezentation(text: String): UInt {
            val split = text.split(":")
            return generateUintTimeReprezentation(split[0].toInt(), split[1].toInt(), split[2].toInt())
        }

        fun addTransferToUintTimeReprezentation(time: UInt): UInt {
            if (time == UInt.MAX_VALUE) {
                return UInt.MAX_VALUE
            }
            return (time + 256u)
        }

        fun addSecondsToUintTimeReprezentation(time: UInt, seconds: UInt): UInt {
            val timeFull = extractSecondsOfDayFromUintTimeReprezentation(time)
            val outTime = timeFull + seconds
            val transfers = time and 7936u
            return transfers or (outTime and 255u) or (outTime shr 8 shl 13)
        }

        fun addMinutesToUintTimeReprezentation(time: UInt, minutes: Int): UInt {
            if (minutes == Int.MAX_VALUE || time == UInt.MAX_VALUE) {
                return UInt.MAX_VALUE
            }
            return addSecondsToUintTimeReprezentation(time, minutes.toUInt() * 60u)
        }

        private fun minusSecondsFromUintTimeReprezentation(time: UInt, seconds: UInt): UInt {
            val timeFull = extractSecondsOfDayFromUintTimeReprezentation(time)
            val outTime = timeFull - seconds
            val transfers = time and 7936u
            return transfers or (outTime and 255u) or (outTime shr 8 shl 13)
        }

        fun minusMinutesFromUintTimeReprezentation(time: UInt, minutes: Int): UInt {
            return minusSecondsFromUintTimeReprezentation(time, minutes.toUInt() * 60u)
        }

        fun extractSecondsOfDayFromUintTimeReprezentation(time: UInt): UInt {
            if (time == UInt.MAX_VALUE) {
                return time
            };
            val lowerBits = time and 255u
            val higherBits = time shr 13 shl 8
            return higherBits or lowerBits
        }

        fun extractTimeFromUintTimeReprezentation(time: UInt): Time {
            val uintTime = extractSecondsOfDayFromUintTimeReprezentation(time)
            val minutes = floor(((uintTime / 60u) % 60u).toDouble()).toInt()
            val seconds = (uintTime % 60u).toInt()
            val hours = floor((uintTime / 3600u).toDouble()).toInt()
            return Time(hours, minutes, seconds)
        }

        fun uintTimeMinusUintTime(time1: UInt, time2: UInt): UInt {
            return extractSecondsOfDayFromUintTimeReprezentation(time1) -
                    extractSecondsOfDayFromUintTimeReprezentation(time2)
        }

        fun timeToMinutes(time: UInt): Double {
            return (time.toDouble() / 60);
        }
    }
}