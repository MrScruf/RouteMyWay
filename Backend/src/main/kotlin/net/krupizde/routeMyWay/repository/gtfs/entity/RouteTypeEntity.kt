package net.krupizde.routeMyWay.repository.gtfs.entity

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class RouteTypeEntity(@Id val routeTypeId: Int, val name: String);