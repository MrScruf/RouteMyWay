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
    private val serviceDayService: ServiceDayService
) {
    private val logger: Logger = LoggerFactory.getLogger(DataProvider::class.java)
    var tripConnections: List<TripConnection> = listOf()
    var footConnections: Map<Int, Set<FootPath>> = mapOf()
    var baseStops: Map<Int, StopBase> = mapOf()
    var baseTrips: Map<Int, TripBase> = mapOf()
    var serviceDays: Map<Int, Map<LocalDate, Boolean>> = mapOf()

    fun getTripConnectionsReversed(
        date: LocalDate,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null
    ) = tripConnections.asReversed().asSequence().filter {
        (vehiclesAllowed == null || vehiclesAllowed.contains(baseTrips[it.tripId]?.routeTypeId)) &&
                (!bikesAllowed || baseTrips[it.tripId]?.bikesAllowed == 1) &&
                (!wheelChairAccessible || baseTrips[it.tripId]?.wheelChairAccessible == 1)
    }.filter { serviceDays[baseTrips[it.tripId]?.serviceId]?.get(date) ?: true }


    fun getTripConnections(
        date: LocalDate,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null
    ) =
        tripConnections.asSequence().filter {
            (vehiclesAllowed == null || vehiclesAllowed.contains(baseTrips[it.tripId]?.routeTypeId)) &&
                    (!bikesAllowed || baseTrips[it.tripId]?.bikesAllowed == 1) &&
                    (!wheelChairAccessible || baseTrips[it.tripId]?.wheelChairAccessible == 1)
        }.filter { serviceDays[baseTrips[it.tripId]?.serviceId]?.get(date) ?: true }


    @Synchronized
    fun reloadData() {
        logger.info("Reloading data cache")
        logger.debug("Reloading trip connections")
        tripConnections = tripConnectionsService.findAllLight().sortedBy { it.departureTime }
        logger.debug("Reloading foot connections")
        val tmpFootConnections = footConnectionsService.findAll()
        val tmpFootConnectionsMap = mutableMapOf<Int, MutableSet<FootPath>>().withDefault { mutableSetOf() }
        tmpFootConnections.forEach {
            //After loading one-sided footpath from DB, save the footpath from both sides
            tmpFootConnectionsMap.getOrPut(it.departureStopId) { mutableSetOf() }.add(it)
            tmpFootConnectionsMap.getOrPut(it.arrivalStopId) { mutableSetOf() }.add(
                FootPath(it.arrivalStopId, it.departureStopId, it.durationInMinutes)
            )
        }
        footConnections = tmpFootConnectionsMap.toMap()
        logger.debug("Reloading service days")
        val tmpServiceDays = serviceDayService.loadAllBase()
        val tmpMapServiceDays = mutableMapOf<Int, MutableMap<LocalDate, Boolean>>()
        tmpServiceDays.forEach {
            tmpMapServiceDays.getOrPut(it.serviceIdInt) { mutableMapOf() }[it.day] = it.willGo
        }
        serviceDays = tmpMapServiceDays
        logger.debug("Reloading stops")
        baseStops = stopService.findAllBase().associateBy { it.id }
        logger.debug("Reloading trips")
        baseTrips = tripService.findAllBase().associateBy { it.id }
        logger.info("Data cache reloaded")
        System.gc()
    }

    fun reloadIfNotLoaded() {
        if (baseTrips.isNotEmpty()) return;
        reloadData()
    }
}