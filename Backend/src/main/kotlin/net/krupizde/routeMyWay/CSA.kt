package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.math.floor
import kotlin.math.min

@Service
class CSA(
    private val dataProvider: DataProvider,
    private val stopService: StopService,
    private val tripService: TripService,
    private val routeService: RouteService,
    private val tripConnectionsService: TripConnectionsService
) {
    private val logger: Logger = LoggerFactory.getLogger(CSA::class.java)


    fun query(departureStopId: Int, arrivalStopId: Int, startTime: UInt): Set<Int> {
        //Setup
        val stops = mutableMapOf<Int, UInt>().withDefault { UInt.MAX_VALUE }
        val trips = mutableSetOf<Int>()
        val tripConnections = dataProvider.baseTripConnections
        val footConnections = dataProvider.footConnections
        footConnections.getValue(departureStopId).forEach {
            stops[it.arrivalStopId] = Utils.addMinutesToTime(startTime, it.durationInMinutes)
        }
        val startIndex = findFirstConnectionIndexByDepartureTime(startTime)
        val relevantConnections = tripConnections.subList(startIndex, tripConnections.size)
        for (connection in relevantConnections) {
            if (stops.getValue(arrivalStopId) <= connection.departureTime) break;
            if (trips.contains(connection.tripId) ||
                stops.getValue(connection.departureStopId) <= connection.departureTime
            ) {
                trips.add(connection.tripId);
                if (connection.arrivalTime < stops.getValue(connection.arrivalStopId)) {
                    footConnections.getValue(connection.arrivalStopId).forEach {
                        stops[it.arrivalStopId] =
                            min(
                                Utils.addMinutesToTime(connection.arrivalTime, it.durationInMinutes),
                                stops.getValue(it.arrivalStopId)
                            )
                    }
                }
            }
        }
        return trips;
    }

    fun findFirstConnectionIndexByDepartureTime(time: UInt): Int {
        var index = dataProvider.baseTripConnections.binarySearch { it.departureTime.compareTo(time) }
        if (index < 0) return (-index - 1)
        while (dataProvider.baseTripConnections[index].departureTime == time) {
            index--;
        }
        return index;
    }

    fun findShortestPathCSAProfile(
        departureStopId: String, arrivalStopId: String, departureDateTime: LocalDateTime,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null,
        numberOfPaths: Int = 1
    ): Paths {
        val departureStopIntId =
            stopService.findByStopId(departureStopId)?.id ?: error("Non-existent stop $departureStopId")
        val arrivalStopIntId =
            stopService.findByStopId(arrivalStopId)?.id ?: error("Non-existent stop $arrivalStopId")
        return findShortestPathCSAProfile(
            departureStopIntId, arrivalStopIntId, departureDateTime, bikesAllowed, wheelChairAccessible,
            vehiclesAllowed, numberOfPaths
        )
    }

    fun findShortestPathCSAProfile(
        departureStopId: Int, arrivalStopId: Int, departureDateTime: LocalDateTime,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null,
        numberOfPaths: Int = 1
    ): Paths {
        val connections = dataProvider.getTripConnectionsReversed(
            departureDateTime.toLocalDate(), bikesAllowed, wheelChairAccessible, vehiclesAllowed
        )
        val durationsToTarget = mutableMapOf<Int, Int>().withDefault { Int.MAX_VALUE }
        dataProvider.footConnections[arrivalStopId]?.forEach {
            durationsToTarget[it.arrivalStopId] = it.durationInMinutes
        } ?: logger.warn("No footpaths to target")
        val departureTimeUint = Utils.generateTime(departureDateTime.toLocalTime())
        val profiles = computeProfiles(
            connections, durationsToTarget, departureStopId, arrivalStopId, departureTimeUint, wheelChairAccessible
        )
        return buildPaths(
            profiles, departureStopId, arrivalStopId, departureTimeUint, durationsToTarget, numberOfPaths
        );
    }


    fun computeProfiles(
        connections: Sequence<TripConnectionBase>, durationsToTarget: Map<Int, Int>, departureStopId: Int,
        arrivalStopId: Int, departureTime: UInt, wheelChairAccessible: Boolean = false

    ): Map<Int, ParetoProfile> {
        val visitedStops = mutableMapOf<Int, ParetoProfile>().withDefault { ParetoProfile() }
        val visitedTrips = mutableMapOf<Int, Pair<UInt, TripConnectionBase?>>().withDefault {
            Pair(UInt.MAX_VALUE, null)
        }
        val reachableTrips = query(departureStopId, arrivalStopId, departureTime)
        for (connection in connections) {
            if (!reachableTrips.contains(connection.tripId)) continue
            val trip = visitedTrips.getValue(connection.tripId)
            val arrivalTimeWhenWalking =
                Utils.addMinutesToTime(connection.arrivalTime, durationsToTarget.getValue(connection.arrivalStopId));
            val arrivalTimeWhenSeatedOnTrip = trip.first
            val arrivalTimeFromCurrentStop =
                arrivalTimeFromStop(visitedStops.getValue(connection.arrivalStopId), connection.arrivalTime);
            val minimalArrivalTime =
                minOf(arrivalTimeWhenWalking, arrivalTimeWhenSeatedOnTrip, arrivalTimeFromCurrentStop);
            val profile = StopProfile(connection.departureTime, minimalArrivalTime)
            if (visitedStops.getValue(departureStopId).dominates(profile) || minimalArrivalTime == UInt.MAX_VALUE)
                continue
            if (!visitedStops.getValue(connection.departureStopId).dominates(profile))
                iterateFootPaths(
                    connection, minimalArrivalTime, trip.second, wheelChairAccessible, visitedStops
                )
            val exitConnection = if (minimalArrivalTime >= trip.first) trip.second else connection
            visitedTrips[connection.tripId] = Pair(minimalArrivalTime, exitConnection)
        }
        return visitedStops
    }

    fun arrivalTimeFromStop(stopProfile: ParetoProfile, arrivalTime: UInt) =
        stopProfile.profiles.find { it.departureTime >= arrivalTime }?.arrivalTime
            ?.let { Utils.addTransferToTime(it) } ?: UInt.MAX_VALUE

    fun iterateFootPaths(
        connection: TripConnectionBase, targetTime: UInt, exitConnection: TripConnectionBase?,
        wheelChairAccessible: Boolean, visitedStops: MutableMap<Int, ParetoProfile>
    ) = dataProvider.footConnections[connection.departureStopId]?.asSequence()?.filter {
        !wheelChairAccessible || (dataProvider.baseStops[it.arrivalStopId]?.wheelChairBoarding == 1
                && dataProvider.baseStops[it.departureStopId]?.wheelChairBoarding == 1)
    }?.forEach {
        visitedStops.getOrPut(it.arrivalStopId) { ParetoProfile() }.add(
            StopProfile(
                Utils.minusMinutesFromTime(connection.departureTime, it.durationInMinutes), targetTime, connection,
                exitConnection
            )
        )
    }

    fun buildPaths(
        profiles: Map<Int, ParetoProfile>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        durationsToTarget: Map<Int, Int>, numberOfPaths: Int = 1
    ): Paths {
        val output = LinkedList<PathPart>()
        var departureTimeTmp = departureTime
        for(i in 1..numberOfPaths) {
            try {
                val path = extractOnePath(profiles, departureStopId, arrivalStopId, departureTimeTmp, durationsToTarget)
                val firstConnection = path.connections.first() // TODO - first should be footPath to first stop
                if (firstConnection is TripConnectionBase)
                    departureTimeTmp = Utils.addSecondsToTime(firstConnection.departureTime, 1u)
                output.add(path)
            }catch(e: java.lang.IllegalStateException){
                if(output.isNotEmpty())break;
                throw e;
            }
        }
        val outStops = output.flatMap { it.stops }.toSet()
        val outTrips = output.flatMap { it.trips }.toSet()
        val outRoutes = output.flatMap { it.routes }.toSet()
        val connections = output.map { it.connections }.toList()
        return Paths(outStops, outTrips, outRoutes, connections)
    }

    fun extractOnePath(
        profiles: Map<Int, ParetoProfile>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        durationsToTarget: Map<Int, Int>
    ): PathPart {
        val departureStop = stopService.findById(departureStopId) ?: error("Non-existent stop $departureStopId")
        val arrivalStop = stopService.findById(arrivalStopId) ?: error("Non-existent stop $arrivalStopId")
        val outStops = mutableSetOf(departureStop, arrivalStop)
        val outTrips = mutableSetOf<Trip>()
        val outRoutes = mutableSetOf<Route>()
        val outConnections = LinkedList<Connection>()
        var departureStopIdTmp = departureStopId
        var departureTimeTmp = departureTime
        do {
            val durationDirectly = durationsToTarget.getValue(departureStopIdTmp).toDouble()
            val profilesTmm = profiles[departureStopIdTmp]?.profiles;
            val profile = profiles[departureStopIdTmp]?.profiles?.first { it.departureTime >= departureTimeTmp }
                ?: error("Non-existent path")
            val enterConnection = profile.enterConnection
            val exitConnection = profile.exitConnection
            val tripLength = Utils.timeToMinutes(Utils.timeMinusTime(profile.arrivalTime, profile.departureTime))
            if (tripLength > durationDirectly) {
                outConnections.add(FootPath(departureStopIdTmp, arrivalStopId, durationDirectly.toInt()))
                break;
            }
            departureStopIdTmp = exitConnection?.arrivalStopId ?: error("Non-existent path");
            departureTimeTmp = exitConnection.arrivalTime
            outStops.add(stopService.findById(enterConnection.departureStopId) ?: error("Non-existent stop"))
            outStops.add(stopService.findById(exitConnection.arrivalStopId) ?: error("Non-existent stop"))
            outTrips.add(tripService.findById(enterConnection.tripId) ?: error("Non-existent trip"))
            outRoutes.add(routeService.findById(outTrips.last().routeId) ?: error("Non-existent route"))
            if (outConnections.isNotEmpty()) {
                val lastConnection = outConnections.last
                if (lastConnection !is TripConnectionBase) error("Last connection cannot be FootPath")
                val durationBetweenStops =
                    Utils.timeMinusTime(enterConnection.departureTime, lastConnection.arrivalTime)
                val durationMin = floor(Utils.timeToMinutes(durationBetweenStops)).toInt()
                outConnections.add(FootPath(lastConnection.arrivalStopId, enterConnection.departureStopId, durationMin))
            }
            outConnections.add(
                TripConnectionBase(
                    enterConnection.departureStopId, exitConnection.arrivalStopId,
                    enterConnection.departureStopDepartureTime, exitConnection.arrivalStopArrivalTime,
                    enterConnection.tripId
                )
            )
        } while (exitConnection?.arrivalStopId != arrivalStopId)
        return PathPart(outStops, outTrips, outRoutes, outConnections)
    }

    fun convertPathsToGtfsPath(paths: Paths): PathGtfs {
        val firstPathTripConnections = tripConnectionsService.findAllByIds(
            paths.paths.first().filterIsInstance<TripConnectionBase>().map { it.tripConnectionId })
        val firstPathFootConnections = paths.paths.first().filterIsInstance<FootPath>()
        val usedStopsIds = firstPathTripConnections.flatMap { listOf(it.departureStopId, it.arrivalStopId) }
        val usedTripsIds = firstPathTripConnections.flatMap { listOf(it.departureStopId, it.arrivalStopId) }
        val usedRoutesIds = firstPathTripConnections.flatMap { listOf(it.departureStopId, it.arrivalStopId) }

        val outStops = paths.stops.toMutableList().filter { !usedStopsIds.contains(it.id) }
        val outTrips = paths.trips.toMutableList().filter { !usedTripsIds.contains(it.id) }
        val outRoutes = paths.routes.toMutableList().filter { !usedRoutesIds.contains(it.id) }
        var stopTimesSequence = 0
        val outStopTimes = firstPathTripConnections.flatMap {
            listOf(
                StopTimeOut(
                    it.tripId, Utils.extractTime(it.departureStopArrivalTime),
                    Utils.extractTime(it.departureStopDepartureTime), it.departureStopId, stopTimesSequence++
                ),
                StopTimeOut(
                    it.tripId, Utils.extractTime(it.arrivalStopArrivalTime),
                    Utils.extractTime(it.arrivalStopDepartureTime), it.arrivalStopId, stopTimesSequence++
                ),
            )
        }
        return PathGtfs(outStops, outTrips, outRoutes, outStopTimes, firstPathFootConnections)
    }
}