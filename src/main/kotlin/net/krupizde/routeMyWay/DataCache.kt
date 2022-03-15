package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class DataCache(
    private val tripConnectionsService: TripConnectionsService,
    private val footConnectionsService: FootConnectionsService,
    private val stopService: StopService,
    private val tripService: TripService,
    private val routeService: RouteService
) {
    private val logger: Logger = LoggerFactory.getLogger(DataCache::class.java)
    var tripConnections: List<TripConnection> = listOf()
    var footConnections: Map<Int, Set<FootPath>> = mapOf()
    var stops: Map<Int, StopLight> = mapOf()
    var trips: Map<Int, TripLight> = mapOf()

    @Synchronized
    fun reloadData() {
        //TODO - concurrency, this method may be called multiple times from multiple threads at the same time, which could start
        // reloading multiple times in a row, which is not desirable
        logger.info("Reloading data cache")
        logger.debug("Reloading trip connections")
        tripConnections = tripConnectionsService.findAllLight().sortedBy { it.departureTime }
        logger.debug("Reloading foot connections")
        val tmp = footConnectionsService.findAll()
        val tmpMap = mutableMapOf<Int, MutableSet<FootPath>>().withDefault { mutableSetOf() }
        for (footConnection in tmp) {
            //After loading one-sided footpath from DB, save the footpath from both sides
            tmpMap.getOrPut(footConnection.departureStopId) { mutableSetOf() }.add(footConnection)
            if (footConnection.arrivalStopId != footConnection.departureStopId)
                tmpMap.getOrPut(footConnection.arrivalStopId) { mutableSetOf() }.add(
                    FootPath(
                        footConnection.arrivalStopId,
                        footConnection.departureStopId,
                        footConnection.durationInMinutes
                    )
                )
        }
        footConnections = tmpMap.toMap()
        logger.debug("Reloading stops")
        stops = stopService.findAllLight().associateBy { it.stopId }
        logger.debug("Reloading trips")
        trips = tripService.findAllLight().associateBy { it.tripId }
        logger.info("Data cache reloaded")
        System.gc()
    }
}