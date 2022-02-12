package net.krupizde.RouteMyWay

interface Connection {
    val departureStation: Stop;
    val arrivalStation: Stop;
    val departureTime: String;
    val arrivalTime: String;
};

data class TripConnection(
    override val departureStation: Stop,
    override val arrivalStation: Stop,
    override val departureTime: String,
    override val arrivalTime: String,
    val trip: Trip
) : Connection

data class FootConnection(
    override val departureStation: Stop,
    override val arrivalStation: Stop,
    override val departureTime: String,
    override val arrivalTime: String,
) : Connection

data class Trip(
    val routeId: String,
    val serviceId: String,
    val tripId: String,
    val tripHeadSign: String,
    val tripShortName: String,
    //val directionId: Int,
    //val shapeId: String,
    //val wheelChairAccessible: Int,
    //val bikesAllowed: Int,
);

data class Stop(
    val id: String,
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
    val arrivalTime: String,
    val departureTime: String,
    val stopId: String,
    val stopSequence: Int
)

data class Route(
    val id: String,
    val shortName: String,
    val longName: String,
    val routeType: Int
);