package net.krupizde.routeMyWay

import java.io.Serializable
import javax.persistence.*
import kotlin.jvm.Transient

@Entity
data class TripConnection(
    val departureStopId: String,
    val arrivalStopId: String,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "hours", column = Column(name = "departureTimeHour")),
        AttributeOverride(name = "minutes", column = Column(name = "departureTimeMinute")),
        AttributeOverride(name = "seconds", column = Column(name = "departureTimeSecond"))
    )
    val departureTime: Time,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "hours", column = Column(name = "arrivalTimeHour")),
        AttributeOverride(name = "minutes", column = Column(name = "arrivalTimeMinute")),
        AttributeOverride(name = "seconds", column = Column(name = "arrivalTimeSecond"))
    )
    val arrivalTime: Time,
    val tripId: String,
    @Id val tripConnectionId: Int = -1
)

@Entity

@IdClass(FootConnectionId::class)
data class FootConnection(
    @Id
    val departureStopId: String = "",
    @Id
    val arrivalStopId: String = "",
    @Column(name = "duration") val durationMinutes: Int = -1
)

data class FootConnectionId(
    val departureStopId: String = "",
    val arrivalStopId: String = ""
) : Serializable;

@Entity
data class Trip(
    @Id val tripId: String,
    val serviceId: String,
    val routeId: String,
    val tripHeadSign: String?,
    val tripShortName: String?,
    @Transient var reachable: Boolean = false
);
@Entity
data class Stop(
    @Id val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationTypeId: Int?,
    @Transient var shortestTime: Time = Time(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
);

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

data class StopTime(
    val tripId: String,
    val arrivalTime: Time,
    val departureTime: Time,
    val stopId: String,
    val stopSequence: Int
)

data class Route(
    val id: String,
    val shortName: String?,
    val longName: String?,
    val routeTypeId: Int
);
@Embeddable
data class Time(var hours: Int, var minutes: Int, var seconds: Int) :
    Comparable<Time> {
    override fun compareTo(other: Time): Int {
        if (this.hours > other.hours) return 1
        if (this.hours < other.hours) return -1
        if (this.minutes > other.minutes) return 1
        if (this.minutes < other.minutes) return -1
        if (this.seconds > other.seconds) return 1
        if (this.seconds < other.seconds) return -1
        return 0
    }

    operator fun plus(addTime: Time): Time {
        val seconds = this.seconds + addTime.seconds
        val minutes = this.minutes + addTime.minutes + (seconds / 60)
        val hours = this.hours + addTime.hours + (minutes / 60)
        return Time(hours % 60, minutes % 60, seconds % 60)
    }

    operator fun plus(addMins: Int): Time {
        val minutes = this.minutes + addMins
        val hours = this.hours + (minutes / 60)
        return Time(hours % 60, minutes % 60, this.seconds)
    }
}

data class PathTripConnection(val departureTime: Time, val arrivalTime: Time, val departureStop: Stop, val arrivalStop: Stop)
data class PathFootConnection(val departureStop: Stop, val arrivalStop: Stop, val durationMinutes: Int)
data class PathStep(val trip: Trip, val tripConnection: PathTripConnection, val footConnection: PathFootConnection)
