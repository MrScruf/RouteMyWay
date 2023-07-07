package net.krupizde.routeMyWay.repository.connections

import net.krupizde.routeMyWay.TripConnection
import org.springframework.data.jpa.repository.JpaRepository

interface TripConnectionJpaRepository : JpaRepository<TripConnection, Int>;