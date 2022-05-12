package net.krupizde.routeMyWay.business

import net.krupizde.routeMyWay.*
import net.krupizde.routeMyWay.utils.Utils
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
    private val delays: MutableMap<Int, MutableMap<LocalDate, Int>> = mutableMapOf()

    fun getTripConnectionsReversed(
        date: LocalDate,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null
    ) = addDelays(
        filter(tripConnections.asReversed().asSequence(), date, bikesAllowed, wheelChairAccessible, vehiclesAllowed),
        date
    )


    fun getTripConnections(
        date: LocalDate,
        bikesAllowed: Boolean = false, wheelChairAccessible: Boolean = false, vehiclesAllowed: Set<Int>? = null
    ) = addDelays(filter(tripConnections.asSequence(), date, bikesAllowed, wheelChairAccessible, vehiclesAllowed), date)

    private fun addDelays(sequence: Sequence<TripConnection>, date: LocalDate) =
        sequence.map {
            val delay = delays[it.tripId]
            if (delay?.contains(date) == true) {
                val delayTime = delay[date] ?: 0
                it.copy(
                    departureTimeDb = Utils.addMinutesToUintTimeReprezentation(it.departureTime, delayTime).toInt(),
                    arrivalTimeDb = Utils.addMinutesToUintTimeReprezentation(it.arrivalTime, delayTime).toInt()
                )
            } else {
                it
            }
        }

    private fun filter(
        sequence: Sequence<TripConnection>, date: LocalDate, bikesAllowed: Boolean, wheelChairAccessible: Boolean,
        vehiclesAllowed: Set<Int>?
    ) = sequence.filter {
        (vehiclesAllowed == null || vehiclesAllowed.contains(baseTrips[it.tripId]?.routeTypeId)) &&
                (!bikesAllowed || baseTrips[it.tripId]?.bikesAllowed == 1) &&
                (!wheelChairAccessible || baseTrips[it.tripId]?.wheelChairAccessible == 1)
    }.filter { serviceDays[baseTrips[it.tripId]?.serviceId]?.get(date) ?: false }

    @Synchronized
    fun reloadData() {
        logger.info("Reloading data cache")
        logger.debug("Reloading trip connections")
        tripConnections = tripConnectionsService.findAll().sortedBy { it.departureTime }
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
        delays.clear()
        logger.info("Data cache reloaded")
        System.gc()
    }

    fun setDelay(tripId: String, date: LocalDate, durationMinutes: Int) {
        val tripIdInt = tripService.findByTripId(tripId)?.id ?: error("Trip not found")
        delays.getOrPut(tripIdInt) { mutableMapOf() }[date] = durationMinutes
    }

    fun clearDelay(tripId: String, date: LocalDate) {
        val tripIdInt = tripService.findByTripId(tripId)?.id ?: error("Trip not found")
        delays[tripIdInt]?.remove(date)
        if (delays[tripIdInt]?.isEmpty() == true) delays.remove(tripIdInt)
    }

    fun clearOldDelays() {
        delays.forEach { delay -> delay.value.entries.removeIf { it.key.isBefore(LocalDate.now()) } }
        delays.entries.removeIf { it.value.isEmpty() }
    }

    fun reloadIfNotLoaded() {
        if (baseTrips.isNotEmpty()) return;
        reloadData()
    }


}