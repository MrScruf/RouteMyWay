package net.krupizde.routeMyWay.Persistence

import net.krupizde.routeMyWay.*
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TripConnectionJpaRepository : JpaRepository<TripConnection, Int>;

interface StopJpaRepository : JpaRepository<Stop, Int> {
    fun findByStopId(stopId: String): Stop?;

    @Query("select s from Stop s where upper(s.name) like upper(?1) or s.stopId like upper(?1)")
    fun findByName(name: String, pageable: Pageable): List<Stop>
}

interface TripJpaRepository : JpaRepository<Trip, Int>;

interface StopLightJpaRepository : JpaRepository<StopBase, Int>;

interface TripLightJpaRepository : JpaRepository<TripBase, Int> {
    @Query("select new TripBase(tl.id, tl.wheelChairAccessible, tl.bikesAllowed, tl.serviceId, r.routeType.routeTypeId) from Trip tl inner join Route r on r.id = tl.routeId")
    fun findAllMapping(): List<TripBase>;
}

interface FootConnectionJpaRepository : JpaRepository<FootPath, FootPathId>;

interface LocationTypeJpaRepository : JpaRepository<LocationType, Int>;

interface RouteJpaRepository : JpaRepository<Route, Int>;

interface RouteTypeJpaRepository : JpaRepository<RouteType, Int>;

interface ServiceDayJpaRepository : JpaRepository<ServiceDay, Int>;

interface ServiceDayBaseJpaRepository : JpaRepository<ServiceDayBase, Int>;

interface ServiceDayTripRelJpaRepository : JpaRepository<ServiceDayTripRel, ServiceDayTripRelId>
