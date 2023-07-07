package net.krupizde.routeMyWay.repository.gtfs.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
@Table(name = "stop")
data class StopEntity(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ManyToOne @JoinColumn(name = "locationTypeId")
    val locationTypeEntity: LocationTypeEntity,
    val wheelChairBoarding: Int?,
    @JsonIgnore @Id @Column(name = "id")
    val id: Int = 0
)