package net.krupizde.routeMyWay.domain.connections.obj

import com.taransit.transport.backend.repository.connection.entity.TripConnectionEntity

data class BasicStopProfileElement(
    val departureTime: Int = Int.MAX_VALUE,
    val arrivalTime: Int = Int.MAX_VALUE,
    val enterConnection: TripConnectionEntity? = null,
    val exitConnection: TripConnectionEntity? = null
) {
    fun dominates(second: BasicStopProfileElement): Boolean =
        departureTime >= second.departureTime && arrivalTime <= second.arrivalTime;
}