package net.krupizde.routeMyWay

import net.krupizde.routeMyWay.Persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

abstract class GeneralRepository<Entity : Any, Id : Any, Repository : JpaRepository<Entity, Id>>() {
    @PersistenceContext
    @Autowired
    protected lateinit var entityManager: EntityManager;

    @Autowired
    protected lateinit var jpaRepository: Repository
    open fun save(entity: Entity): Entity {
        return jpaRepository.save(entity)
    }

    open fun deleteAll() {
        jpaRepository.deleteAllInBatch()
    }

    open fun saveAll(entities: Collection<Entity>): List<Entity> {
        return jpaRepository.saveAll(entities)
    }

    open fun findAll(): List<Entity> {
        return jpaRepository.findAll()
    }

    open fun findById(id: Id): Entity? {
        return jpaRepository.findByIdOrNull(id)
    }

    open fun findAllByIds(ids: List<Id>): List<Entity> {
        return jpaRepository.findAllById(ids)
    }

    open fun findByExample(example: Example<Entity>): Entity? {
        return jpaRepository.findOne(example).orElse(null)
    }

}

@Repository
class TripConnectionRepository() : GeneralRepository<TripConnection, Int, TripConnectionJpaRepository>() {

}

@Repository
class StopRepository() : GeneralRepository<Stop, Int, StopJpaRepository>() {
    fun findByStopId(stopId: String): Stop? {
        return jpaRepository.findByStopId(stopId)
    }

    fun findAllByName(name: String): List<Stop> {
        return jpaRepository.findByName("%${name}%", PageRequest.of(0, 12));
    }
}

@Repository
class TripRepository() : GeneralRepository<Trip, Int, TripJpaRepository>();
@Repository
class StopBaseRepository() : GeneralRepository<StopBase, Int, StopLightJpaRepository>();

@Repository
class TripBaseRepository() : GeneralRepository<TripBase, Int, TripLightJpaRepository>() {
    override fun findAll(): List<TripBase> {
        return jpaRepository.findAllMapping()
    }
}

@Repository
class TripConnectionBaseRepository() :
    GeneralRepository<TripConnection, Int, TripConnectionLightJpaRepository>() {
    override fun findAll(): List<TripConnection> {
        return jpaRepository.findAll()
    }
}

@Repository
class FootConnectionRepository() : GeneralRepository<FootPath, FootPathId, FootConnectionJpaRepository>();

@Repository
class LocationTypeRepository() : GeneralRepository<LocationType, Int, LocationTypeJpaRepository>();

@Repository
class RouteRepository() : GeneralRepository<Route, Int, RouteJpaRepository>();

@Repository
class RouteTypeRepository() : GeneralRepository<RouteType, Int, RouteTypeJpaRepository>();

@Repository
class ServiceDayRepository() : GeneralRepository<ServiceDay, Int, ServiceDayJpaRepository>();

@Repository
class ServiceDayBaseRepository() : GeneralRepository<ServiceDayBase, Int, ServiceDayBaseJpaRepository>();

@Repository
class ServiceDayTripRelRepository() :
    GeneralRepository<ServiceDayTripRel, ServiceDayTripRelId, ServiceDayTripRelJpaRepository>();