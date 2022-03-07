package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

//TODO - Přepsat na csa profile (Priorita na zítra)
//TODO - Refactoring, sestavování cesty udělat rozumněji
@Service
class CSA(
    private val dataCache: DataCache
) {
    private val logger: Logger = LoggerFactory.getLogger(CSA::class.java)
    fun query(departureStopId: String, arrivalStopId: String, startTime: Int): Map<String, Boolean> {
        //Setup
        val stops = mutableMapOf<String, Int>()
        val trips = mutableMapOf<String, Boolean>()
        val tripConnections = dataCache.tripConnections
        val footConnections = dataCache.footConnections
        for (footConnection in footConnections.getValue(departureStopId)) {
            stops[footConnection.arrivalStopId] = Utils.addMinutesToTime(startTime, footConnection.durationInMinutes)
        }
        //TODO - find first reacheable connection using binary search
        val startIndex = firstConnectionIndexByDepartureTime(startTime)
        //Algorithm
        for (connection in tripConnections.subList(startIndex, tripConnections.size)) {
            if (stops.getOrDefault(arrivalStopId, Int.MAX_VALUE) <= connection.departureTime) break;
            if (trips.getOrDefault(connection.tripId, false) || stops.getOrDefault(
                    connection.departureStopId,
                    Int.MAX_VALUE
                ) <= connection.departureTime
            ) {
                trips[connection.tripId] = true;
                if (connection.arrivalTime < stops.getOrDefault(connection.arrivalStopId, Int.MAX_VALUE)) {
                    for (footConnection in footConnections.getValue(connection.arrivalStopId)) {
                        if (Utils.addMinutesToTime(
                                connection.arrivalTime,
                                footConnection.durationInMinutes
                            ) < stops.getOrDefault(
                                footConnection.arrivalStopId,
                                Int.MAX_VALUE
                            )
                        ) {
                            stops[footConnection.arrivalStopId] =
                                Utils.addMinutesToTime(connection.arrivalTime, footConnection.durationInMinutes)
                        }
                    }
                }
            }
        }
        return trips;
    }

    fun firstConnectionIndexByDepartureTime(time: Int): Int {
        var index = dataCache.tripConnections.binarySearch { it.departureTime.compareTo(time) }
        if (index < 0) return -index - 1
        while (dataCache.tripConnections[index].departureTime == time) {
            index--;
        }
        return index;
    }

    //TODO - tripConnections restrictions - Probably done -> bikes allowed, Probably done -> vehicle type - only trains, busses, trams or combinations
    //TODO - footConnections restrictions - wheelchair accessible - add skipping, if stop does not have wheelchair_boarding
    //TODO - arrival time rounding -
    fun findShortestPathCSAProfile(
        departureStopId: String,
        arrivalStopId: String,
        startTime: Int,
        bikesAllowed: Boolean = false,
        wheelChairAccessible: Boolean = false,
        vehiclesAllowed: Set<Int>? = null,
    ): ParetoProfile {
        logger.info("Started setup")
        //SETUP
        //TODO - TripConnection not nullable ?
        val trips = mutableMapOf<String, Pair<Int, TripConnection?>>().withDefault { Pair(Int.MAX_VALUE, null) };
        val reachableTrips = query(departureStopId, arrivalStopId, startTime).withDefault { false }
        val stops = mutableMapOf<String, ParetoProfile>().withDefault {
            ParetoProfile(mutableListOf(StopProfile(Int.MAX_VALUE, Int.MAX_VALUE)))
        };
        val durationsToTarget = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE };
        val connections = dataCache.tripConnections
        val footPaths = dataCache.footConnections
        val routes = dataCache.routes
        val tripsEntities = dataCache.trips
        val stopsEntities = dataCache.stops
        footPaths.getValue(arrivalStopId).forEach { durationsToTarget[it.departureStopId] = it.durationInMinutes }

        logger.info("Started algorithm")
        //ALGORITHM
        for (connection in connections.asReversed()) {
            //Optimization, we dont process unreachable trips.
            if (!reachableTrips.getValue(connection.tripId) ||
                //Allowed vehicles
                (vehiclesAllowed != null && !vehiclesAllowed.contains(routes[tripsEntities.getValue(connection.tripId).routeId]?.routeTypeId)) ||
                //Allowed bikes
                (bikesAllowed && tripsEntities[connection.tripId]?.bikesAllowed == 1) ||
                //Accessible for wheelchairs
                (wheelChairAccessible && tripsEntities[connection.tripId]?.wheelChairAccessible == 1)
            ) continue;
            //arrival time, when walking to the target
            //TODO - Round time to 5 minute chunks down, save original time somehow (speciality inside StopProfile ?)
            val r1 = if (connection.arrivalStopId == arrivalStopId) connection.arrivalTime else Int.MAX_VALUE
            //arrival time, when seated on the current connection
            val r2 = trips.getValue(connection.tripId).first
            //arrival time when transfering
            val r3 = arrivalTimeFromStop(stops.getValue(connection.arrivalStopId), connection.arrivalTime)
            //best arrival time when starting in current connection
            val rc = minOf(r1, r2, r3)
            if (stops.getValue(connection.arrivalStopId).dominates(StopProfile(connection.departureTime, rc))) continue
            footPaths.getValue(connection.departureStopId).asSequence().filter {
                (wheelChairAccessible && it.departureStopId != it.arrivalStopId && //TODO - Bude toto fungovat ?
                        stopsEntities[it.departureStopId]?.wheelChairBoarding == 1 &&
                        stopsEntities[it.arrivalStopId]?.wheelChairBoarding == 1)
            }.forEach {
                stops.getValue(it.departureStopId)
                    .add(
                        StopProfile(
                            Utils.minusMinutesFromTime(connection.departureTime, it.durationInMinutes),
                            rc,
                            connection,
                            //TODO - may be nullable ?
                            trips[connection.tripId]?.second
                        )
                    )
            }
            trips[connection.tripId] = Pair(rc, connection)
        }
        logger.info("Finished algorithm")
        //return buildPath(stops, trips)
        return stops.getValue(arrivalStopId)
    }

    //TODO - isn't copying the profile time too slow ?
    fun arrivalTimeFromStop(stopProfile: ParetoProfile, arrivalTime: Int): Int {
        for (profile in stopProfile.profiles)
            if (profile.departureTime >= arrivalTime) return Utils.addTransferToTime(profile.arrivalTime)
        return Int.MAX_VALUE
    }

    /* The extraction starts by computing the time needed to directly transfer to the target. Doing
    this is trivial without interstop footpaths. With footpaths, we use the D array of the base profile
    algorithm. In the next step, our algorithm determines the first quadruple p after τs in the profile
    S[s] of the source stop s. If directly transferring to the target is faster, then the journey consists of
    a single footpath and there is nothing left to do. Otherwise, p contains the first leg of an optimal
    journey. The algorithm then sets s to l exit
    arr_stop and τs to l exit
    arr_time and iteratively continues to find the
    remaining legs of the output journey. */
    fun buildPath(
        stops: Map<String, ParetoProfile>,
        trips: Map<String, Int>,
        arrivalStopId: String,
        departureStopId: String,
        durationsToTarget: Map<String, Int>
    ): Path {
        TODO()
    }

}