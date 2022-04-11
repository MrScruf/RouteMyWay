package net.krupizde.routeMyWay

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.*
import kotlin.math.max

abstract class Connection(@Transient val name: String = "") {
};

@Entity
@Table(name = "serviceDay")
open class ServiceDayBase(
    open val serviceIdInt: Int,
    @Column(name = "serviceDay") open val day: LocalDate,
    open val willGo: Boolean,
    @Id open val id: Int = -1
)

@Entity
data class ServiceDay(
    val serviceId: String,
    override val serviceIdInt: Int,
    override val day: LocalDate,
    override val willGo: Boolean,
    override val id: Int = -1
) : ServiceDayBase(serviceIdInt, day, willGo, id)

@Entity
@Table(name = "tripConnection")
open class TripConnectionBase(
    open val departureStopId: Int,
    open val arrivalStopId: Int,
    @Column(name = "departureStopDepartureTime") open val departureStopDepartureTime: Int,
    @Column(name = "arrivalStopArrivalTime") open val arrivalStopArrivalTime: Int,
    open val tripId: Int,
    @Id open val tripConnectionId: Int = 0
) : Connection("TripConnection") {
    val departureTime: UInt
        get() = departureStopDepartureTime.toUInt()

    val arrivalTime: UInt
        get() = arrivalStopArrivalTime.toUInt()
}

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
    @Id @Column(name = "id") open val id: Int,
    open val wheelChairBoarding: Int?
)

@Entity
data class Stop(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ManyToOne @JoinColumn(name = "locationTypeId") val locationType: LocationType,
    override val wheelChairBoarding: Int?,
    override val id: Int = 0
) : StopBase(id, wheelChairBoarding);

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

@Entity
@Table(name = "trip")
open class TripBase(
    @Id open val id: Int,
    open val wheelChairAccessible: Int?,
    open val bikesAllowed: Int?,
    open val serviceId: Int,
    @Transient open val routeTypeId: Int = -1
)

@Entity
data class Trip(
    val tripId: String,
    override val serviceId: Int,
    val routeId: Int,
    val tripHeadSign: String?,
    val tripShortName: String?,
    override val wheelChairAccessible: Int?,
    override val bikesAllowed: Int?,
    override val id: Int = 0
) : TripBase(id, wheelChairAccessible, bikesAllowed, serviceId);


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
    val departureTime: LocalTime,
    val arrivalTime: LocalTime,
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