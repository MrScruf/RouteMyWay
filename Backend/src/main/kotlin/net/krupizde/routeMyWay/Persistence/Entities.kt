package net.krupizde.routeMyWay

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import kotlin.math.ceil
import kotlin.math.max


@Entity
@Table(name = "serviceDay")
data class ServiceDayBase(
    val serviceIdInt: Int,
    @Column(name = "serviceDay") val day: LocalDate,
    val willGo: Boolean,
    @Id val id: Int = -1
)

@Entity
@Table(name = "serviceDay")
data class ServiceDay(
    val serviceId: String,
    val serviceIdInt: Int,
    @Column(name = "serviceDay") val day: LocalDate,
    val willGo: Boolean,
    @Id val id: Int = -1
)

@Entity
@Table(name= "ServiceDayTripRel")
@IdClass(ServiceDayTripRelId::class)
data class ServiceDayTripRel(
    @Id val tripId: Int = 0,
    @Id val serviceDayId: Int = 0
)
data class ServiceDayTripRelId(
    val tripId: Int = 0,
    val serviceDayId: Int = 0
) : Serializable;
@Entity
@Table(name = "tripConnection")
data class TripConnection(
    val departureStopId: Int,
    val arrivalStopId: Int,
    @Column(name = "departureTime") val departureTimeDb: Int,
    @Column(name = "arrivalTime") val arrivalTimeDb: Int,
    val tripId: Int,
    @Id val tripConnectionId: Int = 0
) {
    val departureTime: UInt
        get() = departureTimeDb.toUInt();

    val arrivalTime: UInt
        get() = arrivalTimeDb.toUInt();
}


@Entity
@IdClass(FootPathId::class)
data class FootPath(
    @Id val departureStopId: Int = 0,
    @Id val arrivalStopId: Int = 0,
    @Column(name = "duration") val durationInMinutes: Int = -1
)

data class FootPathId(
    val departureStopId: Int = 0,
    val arrivalStopId: Int = 0
) : Serializable;

@Entity
@Table(name = "stop")
data class StopBase(
    @Id @Column(name = "id") val id: Int,
    val wheelChairBoarding: Int?
)

@Entity
@Table(name = "stop")
data class Stop(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ManyToOne @JoinColumn(name = "locationTypeId") val locationType: LocationType,
    val wheelChairBoarding: Int?,
    @Id @Column(name = "id") val id: Int = 0
)

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

@Entity
@Table(name = "trip")
data class TripBase(
    @Id val id: Int,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    val serviceId: Int,
    @Transient val routeTypeId: Int = -1
)

@Entity
@Table(name = "trip")
data class Trip(
    val tripId: String,
    val serviceId: Int,
    val routeId: Int,
    val tripHeadSign: String?,
    val tripShortName: String?,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    @Id val id: Int = 0
)


@Entity
@Table(name = "route")
data class Route(
    val routeId: String,
    val shortName: String?,
    val longName: String?,
    @ManyToOne(optional = false) @JoinColumn(name = "routeTypeId") val routeTypeId: RouteType,
    @Id val id: Int = 0
)

@Entity
data class RouteType(@Id val routeTypeId: Int, val name: String);


data class OutTrip(
    val tripId: String,
    val route: Route,
    val tripHeadSign: String?,
    val tripShortName: String?,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    val id: Int = 0
)

open class Connection(val departureStop: Stop, val arrivalStop: Stop, val name: String);

class OutFootConnection(departureStop: Stop, arrivalStop: Stop, val durationInMinutes: Int) :
    Connection(departureStop, arrivalStop, "Footpath");

class OutTripConnection(
    departureStop: Stop,
    arrivalStop: Stop,
    val departureTime: Time,
    val arrivalTime: Time,
    val trip: OutTrip
) : Connection(departureStop, arrivalStop, "TripConnection")

data class Path(val connections: List<Connection>)

/**
 * Pareto profile holding all profiles of a stop in increasing order of departure times
 */
data class ParetoProfile(
    val profiles: MutableList<StopProfile> = mutableListOf(StopProfile())
) {
    fun dominates(vector: StopProfile): Boolean {
        val fromIndex = profiles.indexOfFirst { it.departureTime >= vector.departureTime }
        for (profile in profiles.listIterator(fromIndex)) {
            if (vector.arrivalTime < profile.arrivalTime) break;
            if (profile.dominates(vector)) return true;
        }
        return false;
    }

    fun add(profile: StopProfile) {
        if (dominates(profile)) return;
        val index = max(profiles.indexOfFirst { it.departureTime >= profile.departureTime }, 0)
        profiles.add(index, profile)
        profiles.subList(0, index).removeIf { profile.dominates(it) }
    }
}

data class StopProfile(
    val departureTime: UInt = UInt.MAX_VALUE,
    val arrivalTime: UInt = UInt.MAX_VALUE,
    val enterConnection: TripConnection = TripConnection(-1, -1, -1, -1, -1),
    val exitConnection: TripConnection? = null
) {
    fun dominates(second: StopProfile): Boolean =
        departureTime >= second.departureTime && arrivalTime <= second.arrivalTime;
}

@JsonSerialize(using = TimeSerializer::class)
data class Time(val hours: Int, val minutes: Int, val seconds: Int) {
    override fun toString(): String {
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${
            seconds.toString().padStart(2, '0')
        }"
    }

    operator fun minus(other: Time): Time {
        val diffSecs = seconds - other.seconds
        val carrySecs = if (diffSecs < 0) 1 else 0
        val diffMinutes = minutes - (other.minutes + carrySecs)
        val carryMinutes = if (diffMinutes < 0) 1 else 0
        val diffHours = hours - (other.hours + carryMinutes)

        val seconds = if (diffSecs < 0) 60 + diffSecs else diffSecs
        val minutes = if (diffMinutes < 0) 60 + diffMinutes else diffMinutes
        val hours = if (diffHours < 0) 24 + diffHours else diffHours
        return Time(hours, minutes, seconds)
    }

    fun toMinutesCeil(): Int {
        return ceil((hours * 60.0) + minutes + (seconds / 60)).toInt()
    }
}

class TimeSerializer() : StdSerializer<Time>(Time::class.java) {
    override fun serialize(value: Time, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.toString())
    }

}