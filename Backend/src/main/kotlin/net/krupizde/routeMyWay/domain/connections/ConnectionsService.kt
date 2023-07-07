package net.krupizde.routeMyWay.domain.connections

import net.krupizde.routeMyWay.domain.connections.utils.CsaUtils
import net.krupizde.routeMyWay.repository.connections.entity.FootConnectionEntity
import net.krupizde.routeMyWay.repository.connections.entity.TripConnectionEntity
import net.krupizde.routeMyWay.repository.gtfs.RouteJpaRepository
import net.krupizde.routeMyWay.repository.gtfs.StopJpaRepository
import net.krupizde.routeMyWay.repository.gtfs.TripJpaRepository
import net.krupizde.routeMyWay.repository.gtfs.entity.RouteTypeEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ConnectionsService(
    private val stopJpaRepository: StopJpaRepository,
    private val tripJpaRepository: TripJpaRepository,
    private val routeJpaRepository: RouteJpaRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(ConnectionsService::class.java)


    protected fun findAccessibleTrips(
        departureStopId: Int,
        arrivalStopId: Int,
        departureTime: UInt,
        connections: List<TripConnectionEntity>,
        footConnections: Map<Int, Set<FootConnectionEntity>>
    ): Set<Int> {
        val stopArrivalTimes = mutableMapOf<Int, UInt>().withDefault { UInt.MAX_VALUE }
        val takenTrips = mutableSetOf<Int>()
        footConnections.getValue(departureStopId).forEach {
            stopArrivalTimes[it.arrivalStopId] = departureTime + it.durationInSeconds.toUInt()
        }
        val limitedConnections = limitSearchedConnections(connections, departureTime)
        for (connection in limitedConnections) {
            val isConnectionAfterBestArrival = stopArrivalTimes.getValue(arrivalStopId) <= connection.departureTime
            if (isConnectionAfterBestArrival) {
                break
            }
            val tripAlreadyTaken = takenTrips.contains(connection.tripId)
            val stopAlreadyVisited = stopArrivalTimes.getValue(connection.departureStopId) <= connection.departureTime
            if (tripAlreadyTaken || stopAlreadyVisited) {
                takenTrips.add(connection.tripId)
                val isNewArrivalTimeBetter =
                    connection.arrivalTime < stopArrivalTimes.getValue(connection.arrivalStopId)
                if (isNewArrivalTimeBetter) {
                    val footConnectionsFromCurrentStop = footConnections.getValue(connection.arrivalStopId)
                    footConnectionsFromCurrentStop.forEach { footConnection ->
                        val minimalArrivalTimeToStop = minOf(
                            connection.arrivalTime + footConnection.durationInSeconds.toUInt(),
                            stopArrivalTimes.getValue(footConnection.arrivalStopId)
                        )
                        stopArrivalTimes[footConnection.arrivalStopId] = minimalArrivalTimeToStop

                    }
                }
            }
        }
        return takenTrips;
    }

    private fun limitSearchedConnections(
        connections: List<TripConnectionEntity>, departureTime: UInt
    ): List<TripConnectionEntity> {
        var index = connections.binarySearch { it.departureTime.compareTo(departureTime) }
        if (index < 0) {
            index *= -1
        }
        while (connections[index].departureTime >= departureTime) {
            index--;
        }
        return connections.subList(index, connections.size)
    }



    protected fun getDirectDurationsFromStopsToTarget(
        arrivalStopId: Int,
        footConnections: Map<Int, Set<FootConnectionEntity>>
    ): Map<Int, Int> {
        return footConnections[arrivalStopId]?.associate { it.arrivalStopId to it.durationInSeconds }.orEmpty()
            .withDefault { Int.MAX_VALUE }
    }


    fun findPath(
        departureStopId: Int,
        arrivalStopId: Int,
        departureTimeInstant: Instant,
        bikesAllowed: Boolean,
        wheelChairAccessible: Boolean,
        vehiclesAllowed: Set<RouteTypeEntity>? = null
    ) {
        val footConnections = footConnectionRepository.loadFootPaths()
        val durationsToTarget = getDirectDurationsFromStopsToTarget(arrivalStopId, footConnections)
        val departureTime = CsaUtils.intTimeFromInstant(departureTimeInstant)
        val tripConnections = tripConnectionSpringRepository.findAllByOrderByDepartureTimeAsc()
        val reachableTrips =
            findAccessibleTrips(departureStopId, arrivalStopId, departureTime, tripConnections, footConnections)
        val reversedConnections = tripConnectionRepository.getTripConnectionsReversed(
            departureTimeInstant, bikesAllowed, wheelChairAccessible, reachableTrips, vehiclesAllowed
        )
        val context = AlgorithmContext(
            footpaths = footConnections,
            durationsToTarget = durationsToTarget,
            departureStopId = departureStopId,
            arrivalStopId = arrivalStopId,
            departureTime = departureTime,
            bikesAllowed = bikesAllowed,
            wheelChairAccessible = wheelChairAccessible,
            vehiclesAllowed = vehiclesAllowed
        );
        val profiles = computeProfiles(reversedConnections, context)
        val connections = extractPathConnectionsSequence(profiles, context, departureTimeInstant)
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
            if (!reachableTrips.contains(connection.tripId)) {
                continue
            }
            val trip = visitedTrips.getValue(connection.tripId)
            val arrivalTimeWhenWalking =
                CsaUtils.addMinutesToUintTimeReprezentation(
                    connection.arrivalTime, durationsToTarget.getValue(connection.arrivalStopId)
                );
            val arrivalTimeWhenSeatedOnTrip = trip.first
            val arrivalTimeFromCurrentStop =
                arrivalTimeFromStop(visitedStops.getValue(connection.arrivalStopId), connection.arrivalTime);
            val minimalArrivalTime =
                minOf(arrivalTimeWhenWalking, arrivalTimeWhenSeatedOnTrip, arrivalTimeFromCurrentStop);
            val profile = StopProfile(connection.departureTime, minimalArrivalTime)
            if (visitedStops.getValue(departureStopId).dominates(profile)) {
                continue
            }
            if (!visitedStops.getValue(connection.departureStopId).dominates(profile)) {
                iterateFootPaths(
                    connection, minimalArrivalTime, trip.second, wheelChairAccessible, visitedStops
                )
            }
            val exitConnection = if (minimalArrivalTime < trip.first) connection else trip.second
            visitedTrips[connection.tripId] = Pair(minimalArrivalTime, exitConnection)
        }
        return visitedStops
    }

    fun arrivalTimeFromStop(stopProfile: ParetoProfile, arrivalTime: UInt) =
        stopProfile.profiles.firstOrNull { it.departureTime >= arrivalTime }?.arrivalTime
            ?.let { CsaUtils.addTransferToUintTimeReprezentation(it) } ?: UInt.MAX_VALUE

    fun iterateFootPaths(
        connection: TripConnection, targetTime: UInt, exitConnection: TripConnection?,
        wheelChairAccessible: Boolean, visitedStops: MutableMap<Int, ParetoProfile>
    ) = dataProvider.footConnections[connection.departureStopId]?.asSequence()?.filter {
        !wheelChairAccessible || (dataProvider.baseStops[it.arrivalStopId]?.wheelChairBoarding == 1
                && dataProvider.baseStops[it.departureStopId]?.wheelChairBoarding == 1)
    }?.forEach {
        visitedStops.getOrPut(it.arrivalStopId) { ParetoProfile() }.add(
            StopProfile(
                CsaUtils.minusMinutesFromUintTimeReprezentation(connection.departureTime, it.durationInMinutes),
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
        val output = mutableListOf<Path>()
        var departureTimeTmp = departureTime
        for (i in 1..numberOfPaths) {
            try {
                val path = extractOnePath(profiles, departureStopId, arrivalStopId, departureTimeTmp, durationsToTarget)
                val firstConnection = path.connections.first() // TODO - first should be footPath to first stop
                if (firstConnection is OutTripConnection) {
                    departureTimeTmp = CsaUtils.addSecondsToUintTimeReprezentation(
                        CsaUtils.generateUintTimeReprezentation(firstConnection.departureTime),
                        1u
                    )
                }
                output.add(path)
            } catch (e: java.lang.IllegalStateException) {
                if (output.isNotEmpty()) {
                    break
                };
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
                CsaUtils.timeToMinutes(CsaUtils.uintTimeMinusUintTime(profile.arrivalTime, profile.departureTime))
            if (tripLength > durationDirectly || tripLength == 0.0) {
                if (durationDirectly.toInt() == Int.MAX_VALUE) {
                    error("Non-existent path")
                }
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
            val depTime = CsaUtils.extractTimeFromUintTimeReprezentation(enterConnection.departureTime)
            val arrTime = CsaUtils.extractTimeFromUintTimeReprezentation(exitConnection.arrivalTime)
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