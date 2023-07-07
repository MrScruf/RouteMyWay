package net.krupizde.routeMyWay.repository.gtfs

import net.krupizde.routeMyWay.repository.gtfs.entity.LocationTypeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LocationTypeJpaRepository : JpaRepository<LocationTypeEntity, Int>;