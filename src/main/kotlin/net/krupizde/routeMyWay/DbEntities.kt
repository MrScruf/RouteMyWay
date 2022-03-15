package net.krupizde.routeMyWay

import java.io.Serializable
import javax.persistence.*

//TODO - spoje ne každý den jezdí trip, přidat den
@Entity
@Table(name = "tripConnection")
data class TripConnection(
    val departureStopId: Int,
    val arrivalStopId: Int,
    @Column(name = "departureTime")
    val departureTimeDb: Int,
    @Column(name = "arrivalTime")
    val arrivalTimeDb: Int,
    val tripId: Int,
    @Id val tripConnectionId: Int = 0
) {
    val departureTime: UInt
        get() = departureTimeDb.toUInt()

    val arrivalTime: UInt
        get() = arrivalTimeDb.toUInt()
}

@Entity
@IdClass(FootPathId::class)
data class FootPath(
    @Id
    val departureStopId: Int = 0,
    @Id
    val arrivalStopId: Int = 0,
    @Column(name = "duration") val durationInMinutes: Int = -1
)

data class FootPathId(
    val departureStopId: Int = 0,
    val arrivalStopId: Int = 0
) : Serializable;

//TODO - wheelchair accessibility - how to store properly
@Entity
@Table(name = "stop")
data class Stop(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ManyToOne @JoinColumn(name="locationTypeId") val locationTypeId: LocationType,
    val wheelChairBoarding: Int?,
    @Id val id: Int = 0
);

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

@Entity
@Table(name = "trip")
data class Trip(
    val tripId: String,
    val serviceId: String,
    val routeId: Int,
    val tripHeadSign: String?,
    val tripShortName: String?,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?,
    @Id val id: Int = 0
);


@Entity
@Table(name = "route")
data class Route(
    val routeId: String,
    val shortName: String?,
    val longName: String?,
    @ManyToOne(optional = false) @JoinColumn(name="routeTypeId")  val routeTypeId: RouteType,
    @Id val id: Int = 0
);
@Entity
data class RouteType(@Id val routeTypeId: Int, val name: String);