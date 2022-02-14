package net.krupizde.routeMyWay

import javax.persistence.*

interface Connection {
    val departureStopId: String;
    val arrivalStopId: String;
    val departureTime: Time;
    val arrivalTime: Time;
};
//TODO místo s entitami přímo pracovat pouze s jejich id, tohle je brutálně pomalé a nevyužiju to
@Entity
data class TripConnection(
    override val departureStopId: String,
    override val arrivalStopId: String,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "hours", column = Column(name = "departureTimeHour")),
        AttributeOverride(name = "minutes", column = Column(name = "departureTimeMinute")),
        AttributeOverride(name = "seconds", column = Column(name = "departureTimeSecond"))
    )
    override val departureTime: Time,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "hours", column = Column(name = "arrivalTimeHour")),
        AttributeOverride(name = "minutes", column = Column(name = "arrivalTimeMinute")),
        AttributeOverride(name = "seconds", column = Column(name = "arrivalTimeSecond"))
    )
    override val arrivalTime: Time,
    val tripId: String,
    @Id val id: Int? = null
) : Connection

data class FootConnection(
    override val departureStopId: String,
    override val arrivalStopId: String,
    override val departureTime: Time,
    override val arrivalTime: Time,
) : Connection

@Entity
data class Trip(
    @Id val tripId: String,
    val serviceId: String,
    val routeId: String,
    val tripHeadSign: String,
    val tripShortName: String,
    //val directionId: Int,
    //val shapeId: String,
    //val wheelChairAccessible: Int,
    //val bikesAllowed: Int,
);
@Entity
data class Stop(
    @Id val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    //val zoneId: String,
    //val stopUrl: String,
    //val locationType: String,
    //val parentStation: String,
    //val wheelchairBoarding: Int
);

data class StopTime(
    val tripId: String,
    val arrivalTime: Time,
    val departureTime: Time,
    val stopId: String,
    val stopSequence: Int
)

data class Route(
    val id: String,
    val shortName: String,
    val longName: String,
    val routeType: Int
);
@Embeddable
data class Time(var hours: Int, var minutes: Int, var seconds: Int) :
    Comparable<Time> {
    override fun compareTo(other: Time): Int {
        if (this.hours > other.hours) return 1
        if (this.hours < other.hours) return -1
        if (this.minutes > other.minutes) return 1
        if (this.minutes < other.minutes) return -1
        if (this.seconds > other.seconds) return 1
        if (this.seconds < other.seconds) return -1
        return 0
    }
}
