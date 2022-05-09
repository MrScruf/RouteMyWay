package net.krupizde.routeMyWay.Business

import net.krupizde.routeMyWay.*
import net.krupizde.routeMyWay.Utils.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.math.min

@Service
class CSA(
    private val dataProvider: DataProvider,
    private val stopService: StopService,
    private val tripService: TripService,
    private val routeService: RouteService,
) {
    private val logger: Logger = LoggerFactory.getLogger(CSA::class.java)


    fun query(
        departureStopId: Int,
        arrivalStopId: Int,
        startTime: UInt,
        connections: Sequence<TripConnection>
    ): Set<Int> {
        //Setup
        val stops = mutableMapOf<Int, UInt>().withDefault { UInt.MAX_VALUE }
        val trips = mutableSetOf<Int>()
        val tripConnections = dataProvider.tripConnections
        val footConnections = dataProvider.footConnections
        footConnections.getValue(departureStopId).forEach {
            stops[it.arrivalStopId] = Utils.addMinutesToUintTimeReprezentation(startTime, it.durationInMinutes)
        }
        for (connection in connections) {
            if (stops.getValue(arrivalStopId) <= connection.departureTime) break;
            if (trips.contains(connection.tripId) ||
                stops.getValue(connection.departureStopId) <= connection.departureTime
            ) {
                trips.add(connection.tripId);
                if (connection.arrivalTime < stops.getValue(connection.arrivalStopId)) {
                    footConnections.getValue(connection.arrivalStopId).forEach {
                        stops[it.arrivalStopId] =
                            min(
                                Utils.addMinutesToUintTimeReprezentation(connection.arrivalTime, it.durationInMinutes),
                                stops.getValue(it.arrivalStopId)
                            )
                    }
                }
            }
        }
        return trips;
    }

    fun findFirstConnectionIndexByDepartureTime(time: UInt): Int {
        var index = dataProvider.tripConnections.binarySearch { it.departureTime.compareTo(time) }
        if (index < 0) return (-index - 1)
        while (dataProvider.tripConnections[index].departureTime == time) {
            index--;
        }
        return index;
    }

    fun findShortestPathCSAProfile(
        departureStopId: String, arrivalStopId: String, departureDateTime: LocalDateTime,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null,
        numberOfPaths: Int = 1, gtfs: Boolean = false
    ): List<Path> {
        val departureStopIntId =
            stopService.findByStopId(departureStopId)?.id ?: error("Non-existent stop $departureStopId")
        val arrivalStopIntId =
            stopService.findByStopId(arrivalStopId)?.id ?: error("Non-existent stop $arrivalStopId")

        return findShortestPathCSAProfile(
            departureStopIntId, arrivalStopIntId, departureDateTime, bikesAllowed, wheelChairAccessible,
            vehiclesAllowed, numberOfPaths
        )
    }


    private fun findShortestPathCSAProfile(
        departureStopId: Int, arrivalStopId: Int, departureDateTime: LocalDateTime,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null,
        numberOfPaths: Int = 1
    ): List<Path> {
        val reversedConnections = dataProvider.getTripConnectionsReversed(
            departureDateTime.toLocalDate(), bikesAllowed, wheelChairAccessible, vehiclesAllowed
        )
        val connections = dataProvider.getTripConnections(
            departureDateTime.toLocalDate(), bikesAllowed, wheelChairAccessible, vehiclesAllowed
        )
        val durationsToTarget = mutableMapOf<Int, Int>().withDefault { Int.MAX_VALUE }
        dataProvider.footConnections[arrivalStopId]?.forEach {
            durationsToTarget[it.arrivalStopId] = it.durationInMinutes
        } ?: logger.warn("No footpaths to target")
        val departureTimeUint = Utils.generateUintTimeReprezentation(departureDateTime.toLocalTime())
        val profiles = computeProfiles(
            reversedConnections, connections, durationsToTarget, departureStopId, arrivalStopId, departureTimeUint,
            wheelChairAccessible,
        )
        return buildPaths(
            profiles, departureStopId, arrivalStopId, departureTimeUint, durationsToTarget, numberOfPaths
        );
    }

    fun computeProfiles(
        reversedConnections: Sequence<TripConnection>, connections: Sequence<TripConnection>,
        durationsToTarget: Map<Int, Int>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        wheelChairAccessible: Boolean = false

    ): Map<Int, ParetoProfile> {
        val visitedStops = mutableMapOf<Int, ParetoProfile>().withDefault { ParetoProfile() }
        val visitedTrips = mutableMapOf<Int, Pair<UInt, TripConnection?>>().withDefault {
            Pair(UInt.MAX_VALUE, null)
        }
        val reachableTrips = query(departureStopId, arrivalStopId, departureTime, connections)
        for (connection in reversedConnections) {
            if (!reachableTrips.contains(connection.tripId)) continue
            val trip = visitedTrips.getValue(connection.tripId)
            val arrivalTimeWhenWalking =
                Utils.addMinutesToUintTimeReprezentation(
                    connection.arrivalTime, durationsToTarget.getValue(connection.arrivalStopId)
                );
            val arrivalTimeWhenSeatedOnTrip = trip.first
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
            val exitConnection = if (minimalArrivalTime < trip.first) connection else trip.second
            visitedTrips[connection.tripId] = Pair(minimalArrivalTime, exitConnection)
        }
        return visitedStops
    }

    fun arrivalTimeFromStop(stopProfile: ParetoProfile, arrivalTime: UInt) =
        stopProfile.profiles.firstOrNull { it.departureTime >= arrivalTime }?.arrivalTime
            ?.let { Utils.addTransferToUintTimeReprezentation(it) } ?: UInt.MAX_VALUE

    fun iterateFootPaths(
        connection: TripConnection, targetTime: UInt, exitConnection: TripConnection?,
        wheelChairAccessible: Boolean, visitedStops: MutableMap<Int, ParetoProfile>
    ) = dataProvider.footConnections[connection.departureStopId]?.asSequence()?.filter {
        !wheelChairAccessible || (dataProvider.baseStops[it.arrivalStopId]?.wheelChairBoarding == 1
                && dataProvider.baseStops[it.departureStopId]?.wheelChairBoarding == 1)
    }?.forEach {
        visitedStops.getOrPut(it.arrivalStopId) { ParetoProfile() }.add(
            StopProfile(
                Utils.minusMinutesFromUintTimeReprezentation(connection.departureTime, it.durationInMinutes),
                targetTime,
                connection,
                exitConnection ?: connection
            )
        )
    }

    fun buildPaths(
        profiles: Map<Int, ParetoProfile>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        durationsToTarget: Map<Int, Int>, numberOfPaths: Int = 1
    ): List<Path> {
        val output = LinkedList<Path>()
        var departureTimeTmp = departureTime
        for (i in 1..numberOfPaths) {
            try {
                val path = extractOnePath(profiles, departureStopId, arrivalStopId, departureTimeTmp, durationsToTarget)
                val firstConnection = path.connections.first() // TODO - first should be footPath to first stop
                if (firstConnection is OutTripConnection)
                    departureTimeTmp = Utils.addSecondsToUintTimeReprezentation(
                        Utils.generateUintTimeReprezentation(firstConnection.departureTime),
                        1u
                    )
                output.add(path)
            } catch (e: java.lang.IllegalStateException) {
                if (output.isNotEmpty()) break;
                throw e;
            }
        }
        return output
    }

    fun extractOnePath(
        profiles: Map<Int, ParetoProfile>, departureStopId: Int, arrivalStopId: Int, departureTime: UInt,
        durationsToTarget: Map<Int, Int>
    ): Path {
        val connections = mutableListOf<Connection>()
        var departureStopIdTmp = departureStopId
        var departureTimeTmp = departureTime
        do {
            val durationDirectly = durationsToTarget.getValue(departureStopIdTmp).toDouble()
            val profile = profiles[departureStopIdTmp]?.profiles?.firstOrNull { it.departureTime >= departureTimeTmp }
                ?: StopProfile()
            val tripLength =
                Utils.timeToMinutes(Utils.uintTimeMinusUintTime(profile.arrivalTime, profile.departureTime))
            if (tripLength > durationDirectly || tripLength == 0.0) {
                if (durationDirectly.toInt() == Int.MAX_VALUE) error("Non-existent path")
                val departureStop = stopService.findById(departureStopIdTmp) ?: error("")
                val arrivalStop = stopService.findById(arrivalStopId) ?: error("")
                connections.add(OutFootConnection(departureStop, arrivalStop, durationDirectly.toInt()))
                break;
            }
            val enterConnection = profile.enterConnection
            val exitConnection = profile.exitConnection ?: error("Non-existent path")
            val departureStop =
                stopService.findById(enterConnection.departureStopId) ?: error("Non-existent departure stop")
            val arrivalStop =
                stopService.findById(exitConnection.arrivalStopId) ?: error("Non-existent arrival stop")
            val tripDb = tripService.findById(enterConnection.tripId) ?: error("Non-existent trip")
            val route = routeService.findById(tripDb.routeId) ?: error("Non-existent route")
            val tripOut = OutTrip(
                tripDb.tripId, route, tripDb.tripHeadSign, tripDb.tripShortName,
                tripDb.wheelChairAccessible, tripDb.bikesAllowed
            )
            val depTime = Utils.extractTimeFromUintTimeReprezentation(enterConnection.departureTime)
            val arrTime = Utils.extractTimeFromUintTimeReprezentation(exitConnection.arrivalTime)
            val lastConnection = connections.lastOrNull()
            if (connections.isNotEmpty() && lastConnection is OutTripConnection) {
                val duration = depTime - lastConnection.arrivalTime
                connections.add(OutFootConnection(lastConnection.arrivalStop, departureStop, duration.toMinutesCeil()))
            }
            val tripConnection = OutTripConnection(departureStop, arrivalStop, depTime, arrTime, tripOut)
            connections.add(tripConnection)
            departureStopIdTmp = exitConnection.arrivalStopId
            departureTimeTmp = exitConnection.arrivalTime
        } while (exitConnection.arrivalStopId != arrivalStopId)
        return Path(connections)
    }
}