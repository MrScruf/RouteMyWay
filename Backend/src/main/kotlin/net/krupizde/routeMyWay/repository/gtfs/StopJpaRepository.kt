package net.krupizde.routeMyWay.repository.gtfs

import net.krupizde.routeMyWay.repository.gtfs.entity.StopEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StopJpaRepository : JpaRepository<StopEntity, Int> {
    fun findByStopId(stopId: String): StopEntity?;

    @Query("select s from StopEntity s where upper(s.name) like upper(?1) or s.stopId like upper(?1)")
    fun findByName(name: String, pageable: Pageable): List<StopEntity>
}