package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.min

//TODO - Přepsat na csa profile (Priorita na zítra)
//TODO - Refactoring, sestavování cesty udělat rozumněji
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
        departureStopId: Int, arrivalStopId: Int, departureDateTime: LocalDateTime,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null
    ): List<Path> {
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
        return buildPaths(profiles, departureStopId, arrivalStopId, departureTimeUint, durationsToTarget);
    }


    fun computeProfiles(
        connections: Sequence<TripConnectionBase>, durationsToTarget: Map<Int, Int>, departureStopId: Int,
        arrivalStopId: Int, departureDateTime: UInt, wheelChairAccessible: Boolean = false

    ): Map<Int, ParetoProfile> {
        val visitedStops = mutableMapOf<Int, ParetoProfile>().withDefault { ParetoProfile() }
        val visitedTrips = mutableMapOf<Int, Pair<UInt, TripConnectionBase?>>().withDefault {
            Pair(UInt.MAX_VALUE, null)
        }
        val reachableTrips = query(departureStopId, arrivalStopId, departureDateTime)
        for (connection in connections) {
            if (!reachableTrips.contains(connection.tripId)) continue
            val trip = visitedTrips[connection.tripId]
            val arrivalTimeWhenWalking =
                Utils.addMinutesToTime(connection.arrivalTime, durationsToTarget.getValue(connection.arrivalStopId));
            val arrivalTimeWhenSeatedOnTrip = trip?.first ?: TODO()
            val arrivalTimeFromCurrentStop =
                arrivalTimeFromStop(visitedStops.getValue(connection.arrivalStopId), connection.arrivalTime);
            val minimalArrivalTime =
                minOf(arrivalTimeWhenWalking, arrivalTimeWhenSeatedOnTrip, arrivalTimeFromCurrentStop);
            val profile = StopProfile(connection.departureTime, minimalArrivalTime)
            if (visitedStops.getValue(departureStopId).dominates(profile)) continue
            if (!visitedStops.getValue(connection.departureStopId).dominates(profile))
                iterateFootPaths(
                    connection, minimalArrivalTime, trip.second, wheelChairAccessible, visitedStops
                )
            val exitConnection = if (minimalArrivalTime >= trip.first) trip.second else connection
            visitedTrips[connection.tripId] = Pair(minimalArrivalTime, exitConnection)
        }
        return visitedStops
    }

    fun arrivalTimeFromStop(stopProfile: ParetoProfile, arrivalTime: UInt): UInt {
        return stopProfile.profiles.find { it.departureTime >= arrivalTime }?.arrivalTime?.let {
            Utils.addTransferToTime(it)
        } ?: UInt.MAX_VALUE
    }

    fun iterateFootPaths(
        connection: TripConnectionBase, targetTime: UInt, exitConnection: TripConnectionBase?,
        wheelChairAccessible: Boolean, visitedStops: MutableMap<Int, ParetoProfile>
    ) = dataProvider.footConnections[connection.departureStopId]?.forEach {
        if (wheelChairAccessible &&
            (dataProvider.baseStops[it.arrivalStopId]?.wheelChairBoarding != 1 ||
                    dataProvider.baseStops[it.departureStopId]?.wheelChairBoarding != 1)
        ) return@forEach
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
    ): List<Path> {
        val output = mutableListOf<Path>()
        repeat(numberOfPaths) {
            val outStops = setOf<Stop>()
            val outTrips = setOf<Trip>()
            val outRoutes = setOf<Route>()
            val outConnections = listOf<Connection>()
            output.add(Path(outStops, outTrips, outRoutes, outConnections))
        }
        return output
    }

    fun extractConnections(
        stops: Map<Int, ParetoProfile>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        durationsToTarget: Map<Int, Int>
    ): Pair<Set<Pair<Int, Int>>, MutableSet<FootPath>> {
        val tripConnectionsIds: MutableSet<Pair<Int, Int>> = mutableSetOf();
        var departureStopIdTmp = departureStopId
        var departureTimeTmp = departureTime
        var prevArrivalStopId = -1
        var prevArrivalTime = -1
        val outFootPaths: MutableSet<FootPath> = mutableSetOf();
        do {
            val durationDirectly = durationsToTarget.getValue(departureStopIdTmp).toDouble()
            val profile = stops[departureStopIdTmp]?.profiles?.first { it.departureTime >= departureTimeTmp }
                ?: error("Non-existent path")
            val tripLength = Utils.timeToMinutes(Utils.timeMinusTime(profile.arrivalTime, profile.departureTime))
            if (tripLength > durationDirectly) {
                outFootPaths.add(FootPath(departureStopIdTmp, arrivalStopId, durationDirectly.toInt()))
                break;
            } else {
                departureStopIdTmp = profile.exitConnection?.arrivalStopId ?: error("Non-existent path");
                departureTimeTmp = profile.exitConnection.arrivalTime
                tripConnectionsIds.add(
                    Pair(profile.enterConnection.tripConnectionId, profile.exitConnection.tripConnectionId)
                )
                if (prevArrivalStopId != -1)
                    outFootPaths.add(
                        FootPath(
                            prevArrivalStopId, profile.enterConnection.departureStopId,
                            ceil(
                                Utils.timeToMinutes(
                                    Utils.timeMinusTime(profile.departureTime, prevArrivalTime.toUInt())
                                )
                            ).toInt()
                        )
                    )
                prevArrivalStopId = profile.exitConnection.arrivalStopId
                prevArrivalTime = profile.exitConnection.arrivalStopArrivalTime
            }
        } while (profile.exitConnection?.arrivalStopId != arrivalStopId)
        return Pair(tripConnectionsIds.toSet(), outFootPaths)
    }

    fun buildPathGtfs(
        stops: Map<Int, ParetoProfile>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        durationsToTarget: Map<Int, Int>
    ): PathGtfs {
        val outStopsIds: MutableSet<Int> = mutableSetOf(departureStopId, arrivalStopId)
        val outTripsIds: MutableSet<Int> = mutableSetOf()
        val outStopTimes: MutableSet<StopTimeOut> = mutableSetOf()
        val connections =
            extractConnections(stops, departureStopId, arrivalStopId, departureTime, durationsToTarget)
        val footConnections = connections.second
        val tripConnectionPairs = connections.first
        var sequence = 1;
        tripConnectionPairs.forEach {
            val enter = tripConnectionsService.findById(it.first)!!
            val exit = tripConnectionsService.findById(it.second)!!
            outTripsIds.add(enter.tripId)
            outStopsIds.add(enter.departureStopId)
            outStopsIds.add(exit.arrivalStopId)
            outStopTimes.add(
                StopTimeOut(
                    enter.tripId, Utils.extractTime(enter.departureStopArrivalTime.toUInt()),
                    Utils.extractTime(enter.departureStopDepartureTime.toUInt()), enter.departureStopId, sequence++
                )
            )
            outStopTimes.add(
                StopTimeOut(
                    exit.tripId, Utils.extractTime(exit.arrivalStopArrivalTime.toUInt()),
                    Utils.extractTime(exit.arrivalStopDepartureTime.toUInt()), exit.arrivalStopId, sequence++
                )
            )
        }
        val outStops = stopService.findAllByIds(outStopsIds.toList())
        val outTrips = tripService.findAllByIds(outTripsIds.toList())
        val outRoutes = routeService.findAllByIds(outTrips.map { it.routeId })
        return PathGtfs(outStops, outTrips, outRoutes, outStopTimes.toList(), footConnections.toList())
    }

}