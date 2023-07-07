package net.krupizde.routeMyWay.domain.connections.obj

import com.taransit.transport.backend.repository.connection.entity.TripConnectionEntity

data class TripTimeConnectionPair (val time: Int, val connection: TripConnectionEntity?) {
}