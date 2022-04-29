package net.krupizde.routeMyWay

import java.time.LocalDate


data class TripConnectionGtfs(
    val departureStopId: String,
    val arrivalStopId: String,
    val departureStopArrivalTime: UInt,
    val departureStopDepartureTime: UInt,
    val arrivalStopArrivalTime: UInt,
    val arrivalStopDepartureTime: UInt,
    val tripId: String,
) {
    fun convertToTripConnection(departureStop: Stop, arrivalStop: Stop, trip: Trip, id: Int): TripConnection {
        return TripConnection(
            departureStop.id, arrivalStop.id, departureStopDepartureTime.toInt(),
            arrivalStopArrivalTime.toInt(), trip.id, id
        )
    }
}

data class StopGtfs(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationTypeId: Int?,
    val wheelChairBoarding: Int?,
) {
    fun convertToStop(locationType: LocationType, id: Int): Stop {
        return Stop(stopId, name, latitude, longitude, locationType, wheelChairBoarding, id)
    }
}

data class FootPathGtfs(
    val departureStopId: String,
    val arrivalStopId: String,
    val durationInMinutes: Int
) {
    fun convertToFootPath(departureStop: Stop, arrivalStop: Stop): FootPath {
        return FootPath(departureStop.id, arrivalStop.id, durationInMinutes)
    }
}

data class StopTimeGtfs(
    val tripId: String,
    val arrivalTime: UInt,
    val departureTime: UInt,
    val stopId: String,
    val stopSequence: Int
)

data class TripGtfs(
    val tripId: String,
    val serviceId: String,
    val routeId: String,
    val tripHeadSign: String?,
    val tripShortName: String?,
    val wheelChairAccessible: Int?,
    val bikesAllowed: Int?
) {
    fun convertToTrip(route: Route, id: Int, serviceIdInt: Int): Trip {
        return Trip(tripId, serviceIdInt, route.id, tripHeadSign, tripShortName, wheelChairAccessible, bikesAllowed, id)
    }
}

data class RouteGtfs(
    val routeId: String,
    val shortName: String?,
    val longName: String?,
    val routeTypeId: Int,
) {
    fun convertToRoute(routeType: RouteType, id: Int): Route {
        return Route(routeId, shortName, longName, routeType, id)
    }
}

data class CalendarGtfs(
    val serviceId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysOfWeek: List<Int>
)

data class CalendarDatesGtfs(
    val serviceId: String,
    val date: LocalDate,
    val exceptionType: Int
)