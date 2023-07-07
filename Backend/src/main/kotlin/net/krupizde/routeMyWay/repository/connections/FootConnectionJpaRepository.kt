package net.krupizde.routeMyWay.repository.connections

import net.krupizde.routeMyWay.FootConnection
import net.krupizde.routeMyWay.FootConnectionId
import org.springframework.data.jpa.repository.JpaRepository

interface FootConnectionJpaRepository : JpaRepository<FootConnection, FootConnectionId>;