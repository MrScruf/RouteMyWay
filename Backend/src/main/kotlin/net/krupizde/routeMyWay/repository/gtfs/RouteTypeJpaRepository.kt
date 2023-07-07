package net.krupizde.routeMyWay.repository.gtfs

import net.krupizde.routeMyWay.repository.gtfs.entity.RouteTypeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RouteTypeJpaRepository : JpaRepository<RouteTypeEntity, Int>;
