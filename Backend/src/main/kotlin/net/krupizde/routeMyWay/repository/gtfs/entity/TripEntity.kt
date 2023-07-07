package net.krupizde.routeMyWay.repository.gtfs.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "trip")
data class TripEntity(
    val tripId: String,
    val serviceId: Int,
    val routeId: Int,
    val tripHeadSign: String?,
    val tripShortName: String?,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    @Id val id: Int = 0
)