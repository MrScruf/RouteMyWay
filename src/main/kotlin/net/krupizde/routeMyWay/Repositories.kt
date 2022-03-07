package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
interface TripConnectionJpaRepository : JpaRepository<TripConnection, Int>;

@Repository
interface StopJpaRepository : JpaRepository<Stop, String>;

@Repository
interface TripJpaRepository : JpaRepository<Trip, String>;

@Repository
interface FootConnectionJpaRepository : JpaRepository<FootPath, FootPathId>;

@Repository
interface LocationTypeJpaRepository : JpaRepository<LocationType, Int>;

@Repository
interface RouteJpaRepository : JpaRepository<Route, String>;

@Repository
interface RouteTypeJpaRepository : JpaRepository<RouteType, Int>;

abstract class GeneralRepository<Entity : Any, Id : Any, Repository : JpaRepository<Entity, Id>>() {
    @PersistenceContext
    @Autowired
    protected lateinit var entityManager: EntityManager;

    @Autowired
    protected lateinit var jpaRepository: Repository
    open fun save(entity: Entity) {
        jpaRepository.save(entity)
    }

    open fun deleteAll() {
        jpaRepository.deleteAll()
    }

    open fun saveAll(entities: Collection<Entity>) {
        jpaRepository.saveAll(entities)
    }

    open fun findAll(): List<Entity> {
        return jpaRepository.findAll()
    }
}

@Repository
class TripConnectionRepository() : GeneralRepository<TripConnection, Int, TripConnectionJpaRepository>() {
    override fun save(tripConnection: TripConnection) {
        val query = entityManager.createNativeQuery(
            "INSERT INTO tripConnection (departureStopId,arrivalStopId,tripId,departureTime,arrivalTime)" +
                    " VALUES (:departureStopId,:arrivalStopId,:tripId,:departureTime,:arrivalTime)"
        )
        query.setParameter("departureStopId", tripConnection.departureStopId)
        query.setParameter("arrivalStopId", tripConnection.arrivalStopId)
        query.setParameter("tripId", tripConnection.tripId)
        query.setParameter("departureTime", tripConnection.departureTime)
        query.setParameter("arrivalTime", tripConnection.arrivalTime)
        query.executeUpdate()
    }
}

@Repository
class StopRepository() : GeneralRepository<Stop, String, StopJpaRepository>();

@Repository
class TripRepository() : GeneralRepository<Trip, String, TripJpaRepository>();

@Repository
class FootConnectionRepository() : GeneralRepository<FootPath, FootPathId, FootConnectionJpaRepository>();

@Repository
class LocationTypeRepository() : GeneralRepository<LocationType, Int, LocationTypeJpaRepository>();

@Repository
class RouteRepository() : GeneralRepository<Route, String, RouteJpaRepository>();

@Repository
class RouteTypeRepository() : GeneralRepository<RouteType, Int, RouteTypeJpaRepository>();