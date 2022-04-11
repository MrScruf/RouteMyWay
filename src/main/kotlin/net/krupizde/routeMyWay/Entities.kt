package net.krupizde.routeMyWay

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import kotlin.math.max

abstract class Connection(@Transient val name: String = "") {
};

@Entity
@Table(name = "serviceDay")
data class ServiceDay(
    val serviceId: String,
    @Column(name = "serviceDay")
    val day: LocalDate,
    val willGo: Boolean,
    @Id val id: Int = -1,
    val tripId: Int = -1
)


@Entity
@Table(name = "tripConnection")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
open class TripConnectionBase(
    val departureStopId: Int,
    val arrivalStopId: Int,
    @Column(name = "departureStopDepartureTime")
    val departureStopDepartureTime: Int,
    @Column(name = "arrivalStopArrivalTime")
    val arrivalStopArrivalTime: Int,
    val tripId: Int,
    @Id val tripConnectionId: Int = 0
) : Connection("TripConnection") {
    val departureTime: UInt
        get() = departureStopDepartureTime.toUInt()

    val arrivalTime: UInt
        get() = arrivalStopArrivalTime.toUInt()
}

//TODO - spoje ne každý den jezdí trip, přidat den
@Entity
class TripConnection(
    departureStopId: Int,
    arrivalStopId: Int,
    @Column(name = "departureStopArrivalTime")
    val departureStopArrivalTime: Int,
    departureStopDepartureTime: Int,
    arrivalStopArrivalTime: Int,
    @Column(name = "arrivalStopDepartureTime")
    val arrivalStopDepartureTime: Int,
    tripId: Int,
    tripConnectionId: Int = 0
) : TripConnectionBase(
    departureStopId, arrivalStopId, departureStopDepartureTime, arrivalStopArrivalTime, tripId, tripConnectionId
) {
}

@Entity
@IdClass(FootPathId::class)
data class FootPath(
    @Id val departureStopId: Int = 0,
    @Id val arrivalStopId: Int = 0,
    @Column(name = "duration") val durationInMinutes: Int = -1
) : Connection("FootPath")

data class FootPathId(
    val departureStopId: Int = 0,
    val arrivalStopId: Int = 0
) : Serializable;

@Entity
@Table(name = "stop")
open class StopBase(
    @Id @Column(name = "id") val id: Int,
    val wheelChairBoarding: Int?
)

@Entity
class Stop(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ManyToOne @JoinColumn(name = "locationTypeId") val locationType: LocationType,
    wheelChairBoarding: Int?,
    id: Int = 0
) : StopBase(id, wheelChairBoarding);

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

@Entity
@Table(name = "trip")
open class TripBase(
    @Id val id: Int,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    val routeTypeId: Int = -1
)

@Entity
class Trip(
    val tripId: String,
    val serviceId: String,
    val routeId: Int,
    val tripHeadSign: String?,
    val tripShortName: String?,
    wheelChairAccessible: Int?,
    bikesAllowed: Int?,
    id: Int = 0
) : TripBase(id, wheelChairAccessible, bikesAllowed);


@Entity
@Table(name = "route")
data class Route(
    val routeId: String,
    val shortName: String?,
    val longName: String?,
    @ManyToOne(optional = false) @JoinColumn(name = "routeTypeId") val routeTypeId: RouteType,
    @Id val id: Int = 0
);
@Entity
data class RouteType(@Id val routeTypeId: Int, val name: String);

data class StopTimeOut(
    val tripId: Int,
    val departureTime: Time,
    val arrivalTime: Time,
    val stopId: Int,
    val stopSequence: Int
)

data class PathGtfs(
    val stops: List<Stop>,
    val trips: List<Trip>,
    val routes: List<Route>,
    val stopTimes: List<StopTimeOut>,
    val footPaths: List<FootPath>
)

data class Path(
    val stops: Set<Stop>,
    val trips: Set<Trip>,
    val routes: Set<Route>,
    val connections: List<Connection>,
)

/**
 * Pareto profile holding all profiles of a stop in increasing order of departure times
 */
data class ParetoProfile(
    val profiles: MutableList<StopProfile> = mutableListOf(StopProfile())
) {
    //TODO - prekontrolovat meze toho, kde se hleda dominance
    fun dominates(vector: StopProfile): Boolean {
        val fromIndex = 0// profiles.indexOfFirst { it.departureTime >= vector.departureTime }
        for (profile in profiles.subList(fromIndex, profiles.size)) {
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
    val enterConnection: TripConnectionBase = TripConnectionBase(-1, -1, -1, -1, -1),
    val exitConnection: TripConnectionBase? = null
) {
    fun dominates(second: StopProfile): Boolean =
        departureTime >= second.departureTime && arrivalTime <= second.arrivalTime;
}

data class Time(val hour: Int, val minute: Int, val second: Int) {
    override fun toString(): String {
        return "$hour:$minute:$second"
    }
}
