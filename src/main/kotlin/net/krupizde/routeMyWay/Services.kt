package net.krupizde.routeMyWay

import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class ConnectionsService(private val tripConnectionRepository: TripConnectionRepository) {
    @Transactional
    fun save(connection: TripConnection) {
        tripConnectionRepository.insert(connection)
    }

    fun saveAll(connections: List<TripConnection>) {

    }

    fun loadAll(): List<TripConnection> {
        return tripConnectionRepository.selectAll()
    }
}
@Service
class StopService(private val stopRepository: StopRepository){
    @Transactional
    fun save(stop: Stop) {
        stopRepository.insert(stop)
    }
}
@Service
class TripService(private val tripRepository: TripRepository){
    @Transactional
    fun save(trip: Trip) {
       tripRepository.insert(trip)
    }
}