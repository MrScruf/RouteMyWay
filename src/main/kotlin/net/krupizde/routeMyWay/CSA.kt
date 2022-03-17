package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.min

//TODO - Přepsat na csa profile (Priorita na zítra)
//TODO - Refactoring, sestavování cesty udělat rozumněji
@Service
class CSA(
    private val dataCache: DataCache,
    private val stopService: StopService,
    private val tripService: TripService,
    private val routeService: RouteService
) {
    private val logger: Logger = LoggerFactory.getLogger(CSA::class.java)
    fun query(departureStopId: Int, arrivalStopId: Int, startTime: UInt): Set<Int> {
        //Setup
        val stops = mutableMapOf<Int, UInt>().withDefault { UInt.MAX_VALUE }
        val trips = mutableSetOf<Int>()
        val tripConnections = dataCache.tripConnections
        val footConnections = dataCache.footConnections
        footConnections.getValue(departureStopId).forEach {
            stops[it.arrivalStopId] = Utils.addMinutesToTime(startTime, it.durationInMinutes)
        }
        val startIndex = firstConnectionIndexByDepartureTime(startTime)
        val sublist = tripConnections.subList(startIndex, tripConnections.size)
        for (connection in sublist) {
            if (stops.getValue(arrivalStopId) <= connection.departureTime) break;
            if (trips.contains(connection.tripId) ||
                stops.getValue(connection.departureStopId) <= connection.departureTime
            ) {
                trips.add(connection.tripId);
                if (connection.arrivalTime < stops.getValue(connection.arrivalStopId)) {
                    for (footConnection in footConnections.getValue(connection.arrivalStopId)) {
                        stops[footConnection.arrivalStopId] =
                            min(
                                Utils.addMinutesToTime(connection.arrivalTime, footConnection.durationInMinutes),
                                stops.getValue(footConnection.arrivalStopId)
                            )
                    }
                }
            }
        }
        return trips;
    }

    fun firstConnectionIndexByDepartureTime(time: UInt): Int {
        var index = dataCache.tripConnections.binarySearch { it.departureTime.compareTo(time) }
        if (index < 0) return (-index - 1)
        while (dataCache.tripConnections[index].departureTime == time) {
            index--;
        }
        return index;
    }

    fun findShortestPathCSAProfile(
        departureStopGtfsId: String,
        arrivalStopGtfsId: String,
        departureTime: UInt,
        bikesAllowed: Boolean = false,
        wheelChairAccessible: Boolean = false,
        vehiclesAllowed: Set<Int>? = null
    ): Path {
        val departureStopId = stopService.findByStopId(departureStopGtfsId)?.id ?: error("Non-existent stop");
        val arrivalStopId = stopService.findByStopId(arrivalStopGtfsId)?.id ?: error("Non-existent stop");
        return findShortestPathCSAProfile(
            departureStopId,
            arrivalStopId,
            departureTime,
            bikesAllowed,
            wheelChairAccessible,
            vehiclesAllowed
        )
    }

    //TODO - tripConnections restrictions - Probably done -> bikes allowed, Probably done -> vehicle type - only trains, busses, trams or combinations
    //TODO - footConnections restrictions - wheelchair accessible - add skipping, if stop does not have wheelchair_boarding
    fun findShortestPathCSAProfile(
        departureStopId: Int,
        arrivalStopId: Int,
        departureTime: UInt,
        bikesAllowed: Boolean = false,
        wheelChairAccessible: Boolean = false,
        vehiclesAllowed: Set<Int>? = null
    ): Path {
        val visitedStops = mutableMapOf<Int, ParetoProfile>().withDefault { ParetoProfile() }
        val visitedTrips = mutableMapOf<Int, Pair<UInt, TripConnection?>>().withDefault { Pair(UInt.MAX_VALUE, null) }
        val durationsToTarget = mutableMapOf<Int, Int>().withDefault { Int.MAX_VALUE }
        dataCache.footConnections[arrivalStopId]?.forEach {
            durationsToTarget[it.arrivalStopId] = it.durationInMinutes
        } ?: logger.warn("No footpaths to target")
        val reacheableTrips = query(departureStopId, arrivalStopId, departureTime)
        for (connection in dataCache.tripConnections.reversed()) {
            if (!reacheableTrips.contains(connection.tripId)) continue
            if ((vehiclesAllowed != null && !vehiclesAllowed.contains(dataCache.trips[connection.tripId]?.routeTypeId))) continue
            val r1 =
                Utils.addMinutesToTime(connection.arrivalTime, durationsToTarget.getValue(connection.arrivalStopId));
            val r2 = visitedTrips.getValue(connection.tripId).first
            val r3 = arrivalTimeFromStop(visitedStops.getValue(connection.arrivalStopId), connection.arrivalTime);
            val rc = minOf(r1, r2, r3);
            val profile = StopProfile(connection.departureTime, rc)
            if (connection.departureStopId == departureStopId) {
                println("Jaj")
            }
            if (visitedStops.getValue(departureStopId).dominates(profile) || rc == UInt.MAX_VALUE) continue
            if (!visitedStops.getValue(connection.arrivalStopId).dominates(profile))
                dataCache.footConnections[connection.departureStopId]?.forEach {
                    if (it.arrivalStopId == departureStopId) {
                        println("Jahudka")
                    }
                    visitedStops.getOrPut(it.arrivalStopId) { ParetoProfile() }.add(
                        StopProfile(
                            Utils.minusMinutesFromTime(connection.departureTime, it.durationInMinutes),
                            rc, connection, visitedTrips[connection.tripId]?.second
                        )
                    )
                }

            visitedTrips[connection.tripId] = Pair(rc, connection)
        }
        return buildPath(visitedStops, departureStopId, arrivalStopId, departureTime, durationsToTarget)
    }

    //TODO - Iz dis gut ?
    fun arrivalTimeFromStop(stopProfile: ParetoProfile, arrivalTime: UInt): UInt {
        return stopProfile.profiles.find { it.departureTime >= arrivalTime }?.arrivalTime?.let {
            Utils.addTransferToTime(it)
        } ?: UInt.MAX_VALUE
    }

    /* The extraction starts by computing the time needed to directly transfer to the target. Doing
    this is trivial without interstop footpaths. With footpaths, we use the D array of the base profile
    algorithm. In the next step, our algorithm determines the first quadruple p after τs in the profile
    S[s] of the source stop s. If directly transferring to the target is faster, then the journey consists of
    a single footpath and there is nothing left to do. Otherwise, p contains the first leg of an optimal
    journey. The algorithm then sets s to l exit
    arr_stop and τs to l exit
    arr_time and iteratively continues to find the
    remaining legs of the output journey.
    */
    fun buildPath(
        stops: Map<Int, ParetoProfile>,
        departureStopId: Int,
        arrivalStopId: Int,
        departureTime: UInt,
        durationsToTarget: Map<Int, Int>
    ): Path {
        val outStopsIds: MutableSet<Int> =
            mutableSetOf(departureStopId, arrivalStopId)
        val outTripsIds: MutableSet<Int> = mutableSetOf()
        val outStopTimes: MutableSet<StopTimeOut> = mutableSetOf()
        val outFootPaths: MutableSet<FootPath> = mutableSetOf()
        var departureStopIdTmp = departureStopId
        var departureTimeTmp = departureTime
        var sequence = 0;
        do {
            val temp = stops[departureStopIdTmp]
            val durationDirectly = durationsToTarget.getValue(departureStopIdTmp).toDouble()
            val profile =
                stops[departureStopIdTmp]?.profiles?.first { it.departureTime >= departureTimeTmp }
                    ?: StopProfile()
            //TODO - co když profile je non-existent a vysledek je 0 ?
            outStopsIds.add(departureStopIdTmp)
            if (Utils.timeToMinutes(Utils.timeMinusTime(profile.arrivalTime, profile.departureTime))
                > durationDirectly
            ) {
                outFootPaths.add(FootPath(departureStopIdTmp, arrivalStopId, durationDirectly.toInt()))
                break;
            } else {
                try {
                    outTripsIds.add(profile.enterConnection.tripId)
                    departureStopIdTmp = profile.exitConnection?.arrivalStopId ?: error("")
                } catch (e: Exception) {
                    logger.error(e.stackTraceToString())
                    break;
                }
                departureTimeTmp = profile.exitConnection.arrivalTime
                outTripsIds.add(profile.exitConnection.tripId)
            }
        } while (profile.exitConnection?.arrivalStopId != arrivalStopId)
        val outStops = stopService.findAllByIds(outStopsIds.toList())
        val outTrips = tripService.findAllByIds(outTripsIds.toList())
        val outRoutes = routeService.findAllByIds(outTrips.map { it.routeId })
        return Path(
            outStops,
            outTrips,
            outRoutes,
            outStopTimes.toList(),
            outFootPaths.toList()
        )
    }

}