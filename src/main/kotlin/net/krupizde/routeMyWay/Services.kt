package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import javax.transaction.Transactional

abstract class GeneralService<Entity : Any, Id : Any,
        Repository : GeneralRepository<Entity, Id, out JpaRepository<Entity, Id>>>() {
    @Autowired
    private lateinit var repository: Repository;

    @Transactional
    open fun save(connection: Entity) {
        repository.save(connection)
    }

    @Transactional
    open fun saveAll(connections: List<Entity>) {
        repository.saveAll(connections)
    }

    @Transactional
    open fun deleteAll() {
        repository.deleteAll()
    }

    @Transactional
    open fun findAll(): List<Entity> {
        return repository.findAll()
    }
}

@Service
class TripConnectionsService() : GeneralService<TripConnection, Int, TripConnectionRepository>();

@Service
class StopService() : GeneralService<Stop, String, StopRepository>();

@Service
class TripService() : GeneralService<Trip, String, TripRepository>();

@Service
class FootConnectionsService() : GeneralService<FootPath, FootPathId, FootConnectionRepository>();

@Service
class LocationTypeService() : GeneralService<LocationType, Int, LocationTypeRepository>();

@Service
class RouteService() : GeneralService<Route, String, RouteRepository>();

@Service
class RouteTypeService() : GeneralService<RouteType, Int, RouteTypeRepository>();