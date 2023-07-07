package net.krupizde.routeMyWay.repository.gtfs

import net.krupizde.routeMyWay.repository.gtfs.entity.TripEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TripJpaRepository : JpaRepository<TripEntity, Int> {
    fun findByTripId(tripId: String): TripEntity?;
}