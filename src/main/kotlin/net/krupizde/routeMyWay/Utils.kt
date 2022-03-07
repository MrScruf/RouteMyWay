package net.krupizde.routeMyWay

class Utils {
    companion object {
        fun generateTime(hours: Int, minutes: Int, seconds: Int): Int {
            val time = (hours * 3600 + minutes * 60 + seconds)
            val higher = time shr 13 shl 13
            val lower = time and 0b11111111
            return 0b100000000 + lower + higher
        }

        fun addTransferToTime(time: Int): Int {
            return time + (1 shl 8)
        }

        fun addSecondsToTime(time: Int, seconds: Int): Int {
            val lowerBits = time and 0b11111111
            val sum = seconds + lowerBits
            return (time and 0b1111100000000) or (sum and 0b11111111) + (sum shr 8 shl 13)
        }

        fun addMinutesToTime(time: Int, minutes: Int): Int {
            return addSecondsToTime(time, minutes * 60)
        }

        fun minusSecondsFromTime(time: Int, seconds: Int): Int {
            val timeFull = time shr 13 shl 8 + (time and 0b11111111)
            val outTime = timeFull - seconds
            val transfers = time and 0b111110000000
            return transfers + (outTime and 0b11111111) + (outTime shr 8 shl 13)
        }

        fun minusMinutesFromTime(time: Int, minutes: Int): Int {
            return minusSecondsFromTime(time, minutes * 60)
        }

        fun extractTime(time: Int): Int {
            val lowerBits = time and 0b11111111
            val higherBits = time shr 13 shl 13
            return higherBits and lowerBits
        }
    }
}