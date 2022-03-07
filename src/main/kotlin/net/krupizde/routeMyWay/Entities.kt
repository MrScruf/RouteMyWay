package net.krupizde.routeMyWay

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import kotlin.math.max

@Entity
data class TripConnection(
    val departureStopId: String,
    val arrivalStopId: String,
    val departureTime: Int,
    val arrivalTime: Int,
    val tripId: String,
    @Id val tripConnectionId: Int = -1
)

@Entity

@IdClass(FootPathId::class)
data class FootPath(
    @Id
    val departureStopId: String = "",
    @Id
    val arrivalStopId: String = "",
    @Column(name = "duration") val durationInMinutes: Int = -1
)

data class FootPathId(
    val departureStopId: String = "",
    val arrivalStopId: String = ""
) : Serializable;
//TODO - wheelchair accessibility - how to store properly
@Entity
data class Trip(
    @Id val tripId: String,
    val serviceId: String,
    val routeId: String,
    val tripHeadSign: String?,
    val tripShortName: String?,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    @Transient var reachable: Boolean = false
);
//TODO - wheelchair accessibility - how to store properly
@Entity
data class Stop(
    @Id val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationTypeId: Int?,
    val wheelChairBoarding: Int?,
    @Transient var shortestTime: Int = Int.MAX_VALUE
);

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

data class StopTime(
    val tripId: String,
    val arrivalTime: Int,
    val departureTime: Int,
    val stopId: String,
    val stopSequence: Int
)

@Entity
data class Route(
    @Id val routeId: String,
    val shortName: String?,
    val longName: String?,
    val routeTypeId: Int
);
@Entity
data class RouteType(@Id val routeTypeId: Int, val name: String);


data class PathTripConnection(
    val departureTime: Int,
    val arrivalTime: Int,
    val departureStop: Stop,
    val arrivalStop: Stop
)

data class Path(
    val stops: List<Stop>,
    val trips: List<Trip>,
    val routes: List<Route>,
    val stopTimes: List<StopTime>,
    val footPaths: List<FootPath>
)

/**
 * Pareto profile holding all profiles of a stop in increasing order of departure times
 */
data class ParetoProfile(val profiles: MutableList<StopProfile> = mutableListOf()) {
    fun dominates(vector: StopProfile): Boolean {
        for (profile in profiles) {
            if (profile.departureTime > vector.departureTime) return false;
            if (profile.dominates(vector)) return true;
        }
        return false;
    }

    //TODO - Slow. Is this the right way ?
    fun add(profile: StopProfile) {
        if (dominates(profile)) return;
        val index = max(profiles.indexOfFirst { it.departureTime >= profile.departureTime }, 0)
        profiles.add(index, profile)
        profiles.subList(0, index).removeIf { profile.dominates(it) }
    }
}

data class StopProfile(
    val departureTime: Int,
    val arrivalTime: Int,
    val enterConnectionId: TripConnection? = null,
    val exitConnectionId: TripConnection? = null
) {
    private fun toList(): List<Int> = listOf(departureTime, arrivalTime)

    fun dominates(second: StopProfile): Boolean {
        val thisList = this.toList()
        val secondList = second.toList()
        var oneSmaller = false;
        for (i in thisList.indices) {
            if (thisList[i] > secondList[i]) return false;
            if (thisList[i] < secondList[i]) oneSmaller = true;
        }
        return oneSmaller;
    }
};