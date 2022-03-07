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
    var footConnections: Map<String, List<FootPath>> = mapOf()
    var stops: Map<String, Stop> = mapOf()
    var trips: Map<String, Trip> = mapOf()
    var routes: Map<String, Route> = mapOf()

    @Synchronized
    fun reloadData() {
        //TODO - concurrency, this method may be called multiple times from multiple threads at the same time, which could start
        // reloading multiple times in a row, which is not desirable
        logger.info("Reloading data cache")
        logger.debug("Reloading trip connections")
        tripConnections = tripConnectionsService.findAll().sortedBy { it.departureTime }
        logger.debug("Reloading foot connections")
        val tmp = footConnectionsService.findAll()
        val tmpMap = mutableMapOf<String, MutableList<FootPath>>()
        for (footConnection in tmp) {
            //After loading one-sided footpath from DB, save the footpath from both sides
            tmpMap.getOrPut(footConnection.arrivalStopId) { mutableListOf() }.add(footConnection)
            tmpMap.getOrPut(footConnection.departureStopId) { mutableListOf() }.add(
                FootPath(
                    footConnection.arrivalStopId,
                    footConnection.departureStopId,
                    footConnection.durationInMinutes
                )
            )
        }
        footConnections = tmpMap.toMap()
        logger.debug("Reloading stops")
        stops = stopService.findAll().associateBy { it.stopId }
        logger.debug("Reloading trips")
        trips = tripService.findAll().associateBy { it.tripId }
        logger.debug("Reloading routes")
        routes = routeService.findAll().associateBy { it.routeId }
        logger.info("Data cache reloaded")
    }
}