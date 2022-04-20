package net.krupizde.routeMyWay

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import kotlin.math.max

abstract class Connection(@Transient val name: String = "") {
};

@Entity
@Table(name = "serviceDay")
open class ServiceDayBase(
    open val serviceIdInt: Int,
    @Column(name = "serviceDay") open val day: LocalDate,
    open val willGo: Boolean,
    @Id open val id: Int = -1
)

@Entity
data class ServiceDay(
    val serviceId: String,
    override val serviceIdInt: Int,
    override val day: LocalDate,
    override val willGo: Boolean,
    override val id: Int = -1
) : ServiceDayBase(serviceIdInt, day, willGo, id)

@Entity
@Table(name = "tripConnection")
open class TripConnectionBase(
    open val departureStopId: Int,
    open val arrivalStopId: Int,
    @Column(name = "departureStopDepartureTime") open val departureStopDepartureTime: Int,
    @Column(name = "arrivalStopArrivalTime") open val arrivalStopArrivalTime: Int,
    open val tripId: Int,
    @Id open val tripConnectionId: Int = 0
) : Connection("TripConnection") {
    val departureTime: UInt
        get() = departureStopDepartureTime.toUInt()

    val arrivalTime: UInt
        get() = arrivalStopArrivalTime.toUInt()

    val depTime: Time
        get() = Utils.extractTime(departureTime)
    val arrTime: Time
        get() = Utils.extractTime(arrivalTime)
}

@Entity
class TripConnection(
    departureStopId: Int,
    arrivalStopId: Int,
    @Column(name = "departureStopArrivalTime")
    val departureStopArrivalTime: Int,
    departureStopDepartureTime: Int,
    arrivalStopArrivalTime: Int,
    @Column(name = "arrivalStopDepartureTime")
    val arrivalStopDepartureTime: Int,
    tripId: Int,
    tripConnectionId: Int = 0
) : TripConnectionBase(
    departureStopId, arrivalStopId, departureStopDepartureTime, arrivalStopArrivalTime, tripId, tripConnectionId
) {
}

@Entity
@IdClass(FootPathId::class)
data class FootPath(
    @Id val departureStopId: Int = 0,
    @Id val arrivalStopId: Int = 0,
    @Column(name = "duration") val durationInMinutes: Int = -1
) : Connection("FootPath"){
    fun toCsv(id:Int):String{
        return "$id,$departureStopId,$arrivalStopId,1,1,$durationInMinutes"
    }
}

data class FootPathId(
    val departureStopId: Int = 0,
    val arrivalStopId: Int = 0
) : Serializable;

@Entity
@Table(name = "stop")
open class StopBase(
    @Id @Column(name = "id") open val id: Int,
    open val wheelChairBoarding: Int?
)

@Entity
data class Stop(
    val stopId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ManyToOne @JoinColumn(name = "locationTypeId") val locationType: LocationType,
    override val wheelChairBoarding: Int?,
    override val id: Int = 0
) : StopBase(id, wheelChairBoarding){
    fun toCsv():String{
        return "$stopId,$name,$latitude,$longitude"
    }
}

@Entity
data class LocationType(@Id val locationTypeId: Int, val name: String)

@Entity
@Table(name = "trip")
open class TripBase(
    @Id open val id: Int,
    open val wheelChairAccessible: Int?,
    open val bikesAllowed: Int?,
    open val serviceId: Int,
    @Transient open val routeTypeId: Int = -1
)

@Entity
data class Trip(
    val tripId: String,
    override val serviceId: Int,
    val routeId: Int,
    val tripHeadSign: String?,
    val tripShortName: String?,
    override val wheelChairAccessible: Int?,
    override val bikesAllowed: Int?,
    override val id: Int = 0
) : TripBase(id, wheelChairAccessible, bikesAllowed, serviceId){
    fun toCsv():String{
        return "$routeId,$serviceId,$tripId,$tripHeadSign,$tripShortName"
    }
}


@Entity
@Table(name = "route")
data class Route(
    val routeId: String,
    val shortName: String?,
    val longName: String?,
    @ManyToOne(optional = false) @JoinColumn(name = "routeTypeId") val routeTypeId: RouteType,
    @Id val id: Int = 0
){
    fun toCsv():String{
        return "$routeId,$shortName,$longName,${routeTypeId.routeTypeId}"
    }
}
@Entity
data class RouteType(@Id val routeTypeId: Int, val name: String);

data class StopTimeOut(
    val tripId: Int,
    val departureTime: Time,
    val arrivalTime: Time,
    val stopId: String,
    val stopSequence: Int
){
    fun toCsv():String{
        return "$tripId,$arrivalTime,$departureTime,$stopId,$stopSequence"
    }
}

data class PathGtfs(
    val stops: List<Stop>,
    val trips: List<Trip>,
    val routes: List<Route>,
    val stopTimes: List<StopTimeOut>,
    val footPaths: List<FootPath>
)

data class Paths(
    val stops: Set<Stop>,
    val trips: Set<Trip>,
    val routes: Set<Route>,
    val paths: List<List<Connection>>
)

data class PathPart(
    val stops: Set<Stop>,
    val trips: Set<Trip>,
    val routes: Set<Route>,
    val connections: List<Connection>
)

/**
 * Pareto profile holding all profiles of a stop in increasing order of departure times
 */
data class ParetoProfile(
    val profiles: MutableList<StopProfile> = mutableListOf(StopProfile())
) {
    //TODO - prekontrolovat meze toho, kde se hleda dominance
    fun dominates(vector: StopProfile): Boolean {
        val fromIndex =  profiles.indexOfFirst { it.departureTime >= vector.departureTime }
        for (profile in profiles.subList(fromIndex, profiles.size)) {
            if (vector.arrivalTime < profile.arrivalTime) break;
            if (profile.dominates(vector)) return true;
        }
        return false;
    }

    fun add(profile: StopProfile) {
        if (dominates(profile)) return;
        val index = max(profiles.indexOfFirst { it.departureTime >= profile.departureTime }, 0)
        profiles.add(index, profile)
        profiles.subList(0, index).removeIf { profile.dominates(it) }
    }
}

data class StopProfile(
    val departureTime: UInt = UInt.MAX_VALUE,
    val arrivalTime: UInt = UInt.MAX_VALUE,
    val enterConnection: TripConnectionBase = TripConnectionBase(-1, -1, -1, -1, -1),
    val exitConnection: TripConnectionBase? = null
) {
    fun dominates(second: StopProfile): Boolean =
        departureTime >= second.departureTime && arrivalTime <= second.arrivalTime;
}

@JsonSerialize(using = TimeSerializer::class)
data class Time(val hours: Int, val minutes: Int, val seconds: Int) {
    override fun toString(): String {
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${
            seconds.toString().padStart(2, '0')
        }"

    }
}

class TimeSerializer() : StdSerializer<Time>(Time::class.java) {
    override fun serialize(value: Time, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.toString())
    }

}