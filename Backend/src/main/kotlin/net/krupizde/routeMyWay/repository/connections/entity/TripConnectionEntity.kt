package net.krupizde.routeMyWay.repository.connections.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "tripConnection")
data class TripConnectionEntity(
    val departureStopId: Int,
    val arrivalStopId: Int,
    @Column(name = "departureTime") val departureTimeDb: Int,
    @Column(name = "arrivalTime") val arrivalTimeDb: Int,
    val tripId: Int,
    @Id @Column(name = "id") val tripConnectionId: Int = 0
) {
    val departureTime: UInt
        get() = departureTimeDb.toUInt();

    val arrivalTime: UInt
        get() = arrivalTimeDb.toUInt();
}