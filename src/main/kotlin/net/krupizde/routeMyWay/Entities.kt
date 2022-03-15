package net.krupizde.routeMyWay

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import kotlin.math.max


@Entity
@Table(name = "trip")
data class TripLight(
    @Id @Column(name = "id") val tripId: Int,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    val routeTypeId: Int
)

@Entity
@Table(name = "stop")
data class StopLight(@Id @Column(name = "id") val stopId: Int, val wheelChairBoarding: Int?)


data class StopTimeOut(
    val tripId: Int,
    val arrivalTime: Time,
    val departureTime: Time,
    val stopId: Int,
    val stopSequence: Int
)

data class Path(
    val stops: List<Stop>,
    val trips: List<Trip>,
    val routes: List<Route>,
    val stopTimes: List<StopTimeOut>,
    val footPaths: List<FootPath>
)

/**
 * Pareto profile holding all profiles of a stop in increasing order of departure times
 */
data class ParetoProfile(
    val profiles: MutableList<StopProfile> = mutableListOf(StopProfile())
) {
    fun dominates(vector: StopProfile, fromIndex: Int = 0): Boolean {
        for (profile in profiles.subList(fromIndex, profiles.size)) {
            if (profile.departureTime > vector.departureTime) return false;
            if (profile.dominates(vector)) return true;
        }
        return false;
    }

    /**
     * Unfortunately, we can no longer guarantee that the departure time of p will be the earliest
    in each profile. A slightly more complex insertion algorithm is therefore needed: Our algorithm
    temporarily removes pairs departing before the new pair. It then inserts p, if nondominated, and
    then reinserts all previously removed pairs that are not dominated by p
     */
    fun add(profile: StopProfile) {
        val index = max(profiles.indexOfFirst { it.departureTime > profile.departureTime }, 0)
        if (dominates(profile, index )) return;
        if (profiles[index].departureTime == profile.departureTime) profiles[index] = profile
        else profiles.add(index, profile)
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
        (departureTime < second.departureTime && arrivalTime <= second.arrivalTime) ||
                (departureTime <= second.departureTime && arrivalTime < second.arrivalTime)
}

data class Time(val hour: Int, val minute: Int, val second: Int) {
    override fun toString(): String {
        return "$hour:$minute:$second"
    }
}
