package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
interface TripConnectionJpaRepository : JpaRepository<TripConnection, Int>;
@Repository
interface TripConnectionLightJpaRepository : JpaRepository<TripConnection, Int>;
@Repository
interface StopJpaRepository : JpaRepository<Stop, Int> {
    fun findByStopId(stopId: String): Stop?;
}

@Repository
interface TripJpaRepository : JpaRepository<Trip, Int>;

@Repository
interface StopLightJpaRepository : JpaRepository<StopLight, Int>;

@Repository
interface TripLightJpaRepository : JpaRepository<TripLight, Int> {
    @Query("select new TripLight(tl.id, tl.wheelChairAccessible, tl.bikesAllowed, r.routeTypeId.routeTypeId) from Trip tl inner join Route r on r.id = tl.routeId")
    fun findAllMapping(): List<TripLight>;
}

@Repository
interface FootConnectionJpaRepository : JpaRepository<FootPath, FootPathId>;

@Repository
interface LocationTypeJpaRepository : JpaRepository<LocationType, Int>;

@Repository
interface RouteJpaRepository : JpaRepository<Route, Int>;

@Repository
interface RouteTypeJpaRepository : JpaRepository<RouteType, Int>;

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
        jpaRepository.deleteAll()
    }

    open fun saveAll(entities: Collection<Entity>): List<Entity> {
        return jpaRepository.saveAll(entities)
    }

    open fun findAll(): List<Entity> {
        return jpaRepository.findAll()
    }

    open fun findAllByIds(ids: List<Id>): List<Entity> {
        return jpaRepository.findAllById(ids)
    }
}

@Repository
class TripConnectionRepository() : GeneralRepository<TripConnection, Int, TripConnectionJpaRepository>() {
    /*override fun save(tripConnection: TripConnection): TripConnection {
        val query = entityManager.createNativeQuery(
            "INSERT INTO tripConnection (departureStopId,arrivalStopId,tripId,departureTime,arrivalTime)" +
                    " VALUES (:departureStopId,:arrivalStopId,:tripId,:departureTime,:arrivalTime)"
        )
        query.setParameter("departureStopId", tripConnection.departureStopId)
        query.setParameter("arrivalStopId", tripConnection.arrivalStopId)
        query.setParameter("tripId", tripConnection.tripId)
        query.setParameter("departureTime", tripConnection.departureTimeDb)
        query.setParameter("arrivalTime", tripConnection.arrivalTimeDb)
        query.executeUpdate()
    }*/
}

@Repository
class StopRepository() : GeneralRepository<Stop, Int, StopJpaRepository>() {
    fun findByStopId(stopId: String): Stop? {
        return jpaRepository.findByStopId(stopId)
    }
}

@Repository
class TripRepository() : GeneralRepository<Trip, Int, TripJpaRepository>();
@Repository
class StopLightRepository() : GeneralRepository<StopLight, Int, StopLightJpaRepository>();

@Repository
class TripLightRepository() : GeneralRepository<TripLight, Int, TripLightJpaRepository>() {
    override fun findAll(): List<TripLight> {
        return jpaRepository.findAllMapping()
    }
}

@Repository
class TripConnectionLightRepository() :
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