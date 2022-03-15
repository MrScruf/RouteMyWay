package net.krupizde.routeMyWay

import kotlin.math.floor

class Utils {
    companion object {
        fun generateTime(hours: Int, minutes: Int, seconds: Int): UInt {
            val time = (hours.toUInt() * 3600u + minutes.toUInt() * 60u + seconds.toUInt())
            val higher = time shr 8 shl 13
            val lower = time and 255u
            return higher or (1u shl 8) or lower
        }

        fun generateTime(time: Time): UInt {
            return generateTime(time.hour, time.minute, time.second)
        }

        fun addTransferToTime(time: UInt): UInt {
            if (time == UInt.MAX_VALUE) return UInt.MAX_VALUE
            return (time + 256u)
        }

        private fun addSecondsToTime(time: UInt, seconds: UInt): UInt {
            val timeFull = extractTimeUint(time)
            val outTime = timeFull + seconds
            val transfers = time and 7936u
            return transfers or (outTime and 255u) or (outTime shr 8 shl 13)
        }

        fun addMinutesToTime(time: UInt, minutes: Int): UInt {
            return addSecondsToTime(time, minutes.toUInt() * 60u)
        }

        private fun minusSecondsFromTime(time: UInt, seconds: UInt): UInt {
            val timeFull = extractTimeUint(time)
            val outTime = timeFull - seconds
            val transfers = time and 7936u
            return transfers or (outTime and 255u) or (outTime shr 8 shl 13)
        }

        fun minusMinutesFromTime(time: UInt, minutes: Int): UInt {
            return minusSecondsFromTime(time, minutes.toUInt() * 60u)
        }

        fun extractTimeUint(time: UInt): UInt {
            if (time == UInt.MAX_VALUE) return time;
            val lowerBits = time and 255u
            val higherBits = time shr 13 shl 8
            return higherBits or lowerBits
        }

        fun extractTime(time: UInt): Time {
            val uintTime = extractTimeUint(time)
            val minutes = floor(((uintTime / 60u) % 60u).toDouble()).toInt()
            val seconds = (uintTime % 60u).toInt()
            val hours = floor((uintTime / 3600u).toDouble()).toInt()
            return Time(hours, minutes, seconds)
        }

        fun timeMinusTime(time1: UInt, time2: UInt): UInt {
            return extractTimeUint(time1) - extractTimeUint(time2)
        }

        fun timeToMinutes(time: UInt): Double {
            return (time / 60u).toDouble();
        }
    }
}