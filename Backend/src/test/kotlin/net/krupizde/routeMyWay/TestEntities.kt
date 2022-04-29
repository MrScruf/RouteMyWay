package net.krupizde.routeMyWay

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntity(@JsonAlias("routes") val routes: List<ResponseEntityRoute>);
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntityRoute(@JsonAlias("legs") val legs: List<ResponseEntityRouteLeg>);
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntityRouteLeg(@JsonAlias("steps") val steps: List<ResponseEntityRouteStep>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntityRouteStep(
    @JsonAlias("travel_mode") val travelMode: String,
    @JsonAlias("duration") val duration: Value,
    @JsonAlias("distance") val distance: Value,
    @JsonAlias("transit_details") val transitDetails: TransitDetails?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitDetails(
    @JsonAlias("arrival_stop") val arrivalStop: TransitDetailsStop,
    @JsonAlias("departure_stop") val departureStop: TransitDetailsStop,
    @JsonAlias("arrival_time") val arrivalTime: Value,
    @JsonAlias("departure_time") val departureTime: Value,
    @JsonAlias("num_stops") val numberOfStops: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitDetailsStop(val name: String, val location: TransitDetailsStopLocation)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitDetailsStopLocation(val lat: Double, val lng: Double)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Value(val value: Int)

data class Walking(val duration: Int, val distance: Int)