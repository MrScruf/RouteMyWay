package net.krupizde.routeMyWay.repository.gtfs

import net.krupizde.routeMyWay.repository.gtfs.entity.RouteEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RouteJpaRepository : JpaRepository<RouteEntity, Int>;
