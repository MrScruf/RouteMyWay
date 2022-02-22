package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

//TODO - Přepsat na csa profile (Priorita na zítra)
//TODO - Refactoring, sestavování cesty udělat rozumněji
@Service
class CSA(
    private val dataCache: DataCache,
    private val stopService: StopService,
    private val tripService: TripService
) {
    private val logger: Logger = LoggerFactory.getLogger(CSA::class.java)
    fun findShortestPath(
        departureStopId: String,
        arrivalStopId: String,
        startTime: Time,
    ): List<PathStep> {
        //Setup
        logger.info("Started setup")
        val loadedStops = stopService.findAll()
        loadedStops.forEach { it.shortestTime = Time(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE) }
        val stops = loadedStops.associateBy { it.stopId }
        logger.info("Loaded stops")
        val loadedTrips = tripService.findAll()
        loadedTrips.forEach { it.reachable = false }
        val trips = loadedTrips.associateBy { it.tripId }
        logger.info("Loaded trips")
        val tripConnections = dataCache.getData().first
        val footConnections = dataCache.getData().second
        logger.info("Loaded connections")
        for (footConnection in footConnections.getValue(departureStopId)) {
            stops.getValue(footConnection.arrivalStopId).shortestTime = startTime + footConnection.durationMinutes
        }
        val reconstruct = HashMap<String, Triple<Trip, TripConnection, FootConnection>>()
        //Algorithm
        logger.info("Started search")
        for (connection in tripConnections) {
            if (stops.getValue(arrivalStopId).shortestTime <= connection.departureTime) break;
            if (trips.getValue(connection.tripId).reachable || stops.getValue(connection.departureStopId).shortestTime <= connection.departureTime) {
                trips[connection.tripId]?.reachable = true;
                if (connection.arrivalTime < stops.getValue(connection.arrivalStopId).shortestTime) {
                    for (footConnection in footConnections.getValue(connection.arrivalStopId)) {
                        if (connection.arrivalTime + footConnection.durationMinutes < stops.getValue(footConnection.arrivalStopId).shortestTime) {
                            stops.getValue(footConnection.arrivalStopId).shortestTime =
                                connection.arrivalTime + footConnection.durationMinutes
                            reconstruct[footConnection.arrivalStopId] =
                                Triple(trips.getValue(connection.tripId), connection, footConnection)
                        }
                    }
                }
            }
        }
        val path = LinkedList<PathStep>()
        var step = reconstruct[arrivalStopId]
        while (step != null) {
            path.push(
                PathStep(
                    step.first,
                    PathTripConnection(
                        step.second.departureTime,
                        step.second.arrivalTime,
                        stops.getValue(step.second.departureStopId),
                        stops.getValue(step.second.arrivalStopId)
                    ),
                    PathFootConnection(
                        stops.getValue(step.third.departureStopId),
                        stops.getValue(step.third.arrivalStopId),
                        step.third.durationMinutes
                    )
                )
            )
            step = reconstruct[step.second.departureStopId]
        }
        logger.info("Finished search")
        return path.toList()
    }
}