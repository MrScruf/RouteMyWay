package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
interface TripConnectionJpaRepository : JpaRepository<TripConnection, Int>;
@Repository
interface StopJpaRepository : JpaRepository<Stop, String>

@Repository
interface TripJpaRepository : JpaRepository<Trip, String>

@Repository
interface FootConnectionJpaRepository : JpaRepository<FootConnection, FootConnectionId>

@Repository
interface LocationTypeJpaRepository : JpaRepository<LocationType, Int>

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
        jpaRepository.flush()
    }

    open fun findAll(): List<Entity> {
        return jpaRepository.findAll()
    }
}

@Repository
class TripConnectionRepository() : GeneralRepository<TripConnection, Int, TripConnectionJpaRepository>() {
    override fun save(tripConnection: TripConnection) {
        val query = entityManager.createNativeQuery(
            "INSERT INTO tripConnection (departureStopId,arrivalStopId,tripId,departureTimeHour," +
                    "departureTimeMinute,departureTimeSecond,arrivalTimeHour,arrivalTimeMinute,arrivalTimeSecond)" +
                    " VALUES (:departureStopId,:arrivalStopId,:tripId,:departureTimeHour," +
                    ":departureTimeMinute,:departureTimeSecond,:arrivalTimeHour,:arrivalTimeMinute,:arrivalTimeSecond)"
        )
        query.setParameter("departureStopId", tripConnection.departureStopId)
        query.setParameter("arrivalStopId", tripConnection.arrivalStopId)
        query.setParameter("tripId", tripConnection.tripId)
        query.setParameter("departureTimeHour", tripConnection.departureTime.hours)
        query.setParameter("departureTimeMinute", tripConnection.departureTime.minutes)
        query.setParameter("departureTimeSecond", tripConnection.departureTime.seconds)
        query.setParameter("arrivalTimeHour", tripConnection.arrivalTime.hours)
        query.setParameter("arrivalTimeMinute", tripConnection.arrivalTime.minutes)
        query.setParameter("arrivalTimeSecond", tripConnection.arrivalTime.seconds)
        query.executeUpdate()
    }

    override fun findAll(): List<TripConnection> {
        return jpaRepository.findAll(
            Sort.by(
                Sort.Direction.ASC,
                "departureTime.hours",
                "departureTime.minutes",
                "departureTime.seconds"
            )
        )
    }
}

@Repository
class StopRepository() : GeneralRepository<Stop, String, StopJpaRepository>();

@Repository
class TripRepository() : GeneralRepository<Trip, String, TripJpaRepository>();

@Repository
class FootConnectionRepository() : GeneralRepository<FootConnection, FootConnectionId, FootConnectionJpaRepository>();

@Repository
class LocationTypeRepository() : GeneralRepository<LocationType, Int, LocationTypeJpaRepository>();