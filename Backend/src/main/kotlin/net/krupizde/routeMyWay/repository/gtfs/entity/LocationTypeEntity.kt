package net.krupizde.routeMyWay.repository.gtfs.entity

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class LocationTypeEntity(@Id val locationTypeId: Int, val name: String)