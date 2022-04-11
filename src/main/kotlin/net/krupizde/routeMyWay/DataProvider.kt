package net.krupizde.routeMyWay

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class DataProvider(
    private val tripConnectionsService: TripConnectionsService,
    private val footConnectionsService: FootConnectionsService,
    private val stopService: StopService,
    private val tripService: TripService,
    private val routeService: RouteService
) {
    private val logger: Logger = LoggerFactory.getLogger(DataProvider::class.java)
    var baseTripConnections: List<TripConnectionBase> = listOf()
    var footConnections: Map<Int, Set<FootPath>> = mapOf()
    var baseStops: Map<Int, StopBase> = mapOf()
    var baseTrips: Map<Int, TripBase> = mapOf()

    fun getTripConnectionsReversed(
        date: LocalDate,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null
    ) = baseTripConnections.asReversed().asSequence().filter {
        (vehiclesAllowed == null || vehiclesAllowed.contains(baseTrips[it.tripId]?.routeTypeId)) &&
                (!bikesAllowed || baseTrips[it.tripId]?.bikesAllowed == 1) &&
                (!wheelChairAccessible || baseTrips[it.tripId]?.wheelChairAccessible == 1)
    }.filter { true }//TODO - callendar dates filtering

    @Synchronized
    fun reloadData() {
        //TODO - concurrency, this method may be called multiple times from multiple threads at the same time, which could start
        // reloading multiple times in a row, which is not desirable
        logger.info("Reloading data cache")
        logger.debug("Reloading trip connections")
        baseTripConnections = tripConnectionsService.findAllLight().sortedBy { it.departureTime }
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
        baseStops = stopService.findAllLight().associateBy { it.id }
        logger.debug("Reloading trips")
        baseTrips = tripService.findAllLight().associateBy { it.id }
        logger.info("Data cache reloaded")
        System.gc()
    }
}