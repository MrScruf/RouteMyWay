package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import javax.transaction.Transactional

abstract class GeneralService<Entity : Any, Id : Any,
        Repository : GeneralRepository<Entity, Id, out JpaRepository<Entity, Id>>>() {
    @Autowired
    protected lateinit var repository: Repository;

    @Transactional
    open fun save(connection: Entity): Entity {
        return repository.save(connection)
    }

    @Transactional
    open fun saveAll(connections: List<Entity>): List<Entity> {
        return repository.saveAll(connections)
    }

    @Transactional
    open fun deleteAll() {
        repository.deleteAll()
    }

    @Transactional
    open fun findAll(): List<Entity> {
        return repository.findAll()
    }

    @Transactional
    open fun findById(id: Id): Entity? {
        return repository.findById(id)
    }

    @Transactional
    open fun findAllByIds(ids: List<Id>): List<Entity> {
        return repository.findAllByIds(ids)
    }
}

@Service
class TripConnectionsService(val tripConnectionBaseRepository: TripConnectionBaseRepository) :
    GeneralService<TripConnection, Int, TripConnectionRepository>() {
    fun findAllLight(): List<TripConnectionBase> {
        return tripConnectionBaseRepository.findAll()
    }
}

@Service
class StopService(val stopBaseRepository: StopBaseRepository) : GeneralService<Stop, Int, StopRepository>() {
    fun findAllBase(): List<StopBase> {
        return stopBaseRepository.findAll()
    }

    fun findByStopId(stopId: String): Stop? {
        return repository.findByStopId(stopId)
    }

    fun findAllByName(name: String): List<Stop> {
        return repository.findAllByName(name)
    }
}

@Service
class TripService(val tripBaseRepository: TripBaseRepository) : GeneralService<Trip, Int, TripRepository>() {
    fun findAllBase(): List<TripBase> {
        return tripBaseRepository.findAll()
    }
}

@Service
class FootConnectionsService() : GeneralService<FootPath, FootPathId, FootConnectionRepository>();

@Service
class LocationTypeService() : GeneralService<LocationType, Int, LocationTypeRepository>();

@Service
class RouteService() : GeneralService<Route, Int, RouteRepository>();

@Service
class RouteTypeService() : GeneralService<RouteType, Int, RouteTypeRepository>();

@Service
class ServiceDayService(val serviceDayBaseRepository: ServiceDayBaseRepository) :
    GeneralService<ServiceDay, Int, ServiceDayRepository>() {
    fun loadAllBase(): List<ServiceDayBase> {
        return serviceDayBaseRepository.findAll();
    }
}