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
        logger.info("Started setup")
        //SETUP
        //TODO - TripConnection not nullable ?
        val trips = mutableMapOf<Int, Pair<UInt, TripConnection?>>().withDefault { Pair(UInt.MAX_VALUE, null) };
        logger.info("Running basic CSA")
        val reachableTrips = query(departureStopId, arrivalStopId, departureTime)
        logger.info("Finished basic CSA")
        val stops = mutableMapOf<Int, ParetoProfile>().withDefault {
            ParetoProfile(mutableListOf(StopProfile(UInt.MAX_VALUE, UInt.MAX_VALUE)))
        };
        val durationsToTarget = mutableMapOf<Int, UInt>().withDefault { UInt.MAX_VALUE };
        val footPaths = dataCache.footConnections
        footPaths.getValue(arrivalStopId)
            .forEach { durationsToTarget[it.arrivalStopId] = it.durationInMinutes.toUInt() }

        logger.info("Started algorithm")
        //ALGORITHM
        val reversed = dataCache.tripConnections.asReversed()
        for (connection in reversed) {
            //if (connection.departureTime < departureTime) break;
            //Optimization, we dont process unreachable trips.
            if (!reachableTrips.contains(connection.tripId)) continue
            if ((vehiclesAllowed != null && !vehiclesAllowed.contains(dataCache.trips[connection.tripId]?.routeTypeId)) ||
                (bikesAllowed && dataCache.trips[connection.tripId]?.bikesAllowed == 1) ||
                (wheelChairAccessible && dataCache.trips[connection.tripId]?.wheelChairAccessible == 1)
            ) continue;
            //arrival time, when walking to the target
            val r1 = if (durationsToTarget.getValue(connection.arrivalStopId) == UInt.MAX_VALUE) UInt.MAX_VALUE
            else (Utils.addMinutesToTime(
                connection.arrivalTime,
                durationsToTarget.getValue(connection.arrivalStopId).toInt()
            ))
            //arrival time, when seated on the current connection
            val r2 = trips.getValue(connection.tripId).first
            //arrival time when transfering
            val r3 = arrivalTimeFromStop(stops.getValue(connection.arrivalStopId), connection.arrivalTime)
            //best arrival time when starting in current connection
            val rc = minOf(r1, r2, r3)
            val profile = StopProfile(connection.departureTime, rc)
            // Source domination
            if (stops.getValue(departureStopId).dominates(profile)) continue
            // Handle transfers and initial footpaths
            if (!stops.getValue(connection.arrivalStopId).dominates(profile))
                footPaths.getValue(connection.departureStopId).forEach {
                    if (wheelChairAccessible && it.departureStopId != it.arrivalStopId &&
                        (dataCache.stops[it.departureStopId]?.wheelChairBoarding == 1) &&
                        (dataCache.stops[it.arrivalStopId]?.wheelChairBoarding == 1)
                    ) return@forEach
                    if(it.departureStopId == 3407){
                        println("Jahudska")
                    }
                    stops.getOrPut(it.arrivalStopId) { ParetoProfile() }.add(
                        StopProfile(
                            Utils.minusMinutesFromTime(connection.departureTime, it.durationInMinutes),
                            rc,
                            connection,
                            trips[connection.tripId]?.second
                        )
                    )
                }
            val storeConnection =
                /*if (trips.getValue(connection.tripId).first < rc) trips[connection.tripId]!!.second else */connection
            trips[connection.tripId] = Pair(rc, storeConnection)
        }
        logger.info("Finished algorithm")
        return buildPath(stops, arrivalStopId, departureStopId, departureTime, durationsToTarget)
    }

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
        arrivalStopId: Int,
        departureStopId: Int,
        departureTime: UInt,
        durationsToTarget: Map<Int, UInt>
    ): Path {
        val outStopsIds: MutableSet<Int> =
            mutableSetOf(departureStopId, arrivalStopId)
        val outTripsIds: MutableSet<Int> = mutableSetOf()
        val outRoutes: MutableSet<Route> = mutableSetOf()
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
            if (Utils.timeToMinutes(Utils.timeMinusTime(profile.arrivalTime, profile.departureTime))
                > durationDirectly
            ) {
                outFootPaths.add(FootPath(departureStopIdTmp, arrivalStopId, durationDirectly.toInt()))
                break;
            } else {
                try {
                    departureStopIdTmp = profile.exitConnection?.arrivalStopId ?: error("")
                } catch (e: Exception) {
                    logger.error(e.stackTraceToString())
                    break;
                }
                departureTimeTmp = profile.exitConnection.arrivalTime
                outStopsIds.add(profile.exitConnection.arrivalStopId)
                outStopsIds.add(profile.exitConnection.departureStopId)
                outStopsIds.add(profile.enterConnection.departureStopId)
                outStopsIds.add(profile.enterConnection.arrivalStopId)
                outTripsIds.add(profile.exitConnection.tripId)
                outTripsIds.add(profile.enterConnection.tripId)
                outStopTimes.add(
                    StopTimeOut(
                        profile.enterConnection.tripId,
                        Utils.extractTime(profile.enterConnection.departureTime),
                        Utils.extractTime(profile.enterConnection.departureTime),
                        profile.enterConnection.departureStopId,
                        sequence++
                    )
                )
                outStopTimes.add(
                    StopTimeOut(
                        profile.enterConnection.tripId,
                        Utils.extractTime(profile.enterConnection.arrivalTime),
                        Utils.extractTime(profile.enterConnection.arrivalTime),
                        profile.enterConnection.arrivalStopId,
                        sequence++
                    )
                )
                outStopTimes.add(
                    StopTimeOut(
                        profile.exitConnection.tripId,
                        Utils.extractTime(profile.exitConnection.departureTime),
                        Utils.extractTime(profile.exitConnection.departureTime),
                        profile.exitConnection.departureStopId,
                        sequence++
                    )
                )
                outStopTimes.add(
                    StopTimeOut(
                        profile.exitConnection.tripId,
                        Utils.extractTime(profile.exitConnection.arrivalTime),
                        Utils.extractTime(profile.exitConnection.arrivalTime),
                        profile.exitConnection.arrivalStopId,
                        sequence++
                    )
                )
            }
        } while (profile.exitConnection?.arrivalStopId != arrivalStopId)

        return Path(
            stopService.findAllByIds(outStopsIds.toList()),
            tripService.findAllByIds(outTripsIds.toList()),
            outRoutes.toList(),
            outStopTimes.toList(),
            outFootPaths.toList()
        )
    }

}