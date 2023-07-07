package net.krupizde.routeMyWay.repository.gtfs.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "route")
data class RouteEntity(
    val routeId: String,
    val shortName: String?,
    val longName: String?,
    @ManyToOne(optional = false)
    @JoinColumn(name = "routeTypeId")
    val routeType: RouteTypeEntity,
    @JsonIgnore @Id val id: Int = 0
)