package net.krupizde.routeMyWay.repository.connections.entity

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass

@Entity
@IdClass(FootConnectionEntity.FootConnectionId::class)
data class FootConnectionEntity(
    @Id val departureStopId: Int = 0,
    @Id val arrivalStopId: Int = 0,
    @Column(name = "durationInSeconds") val durationInSeconds: Int = -1
) {
    data class FootConnectionId(
        val departureStopId: Int = 0,
        val arrivalStopId: Int = 0
    ) : Serializable;
}

