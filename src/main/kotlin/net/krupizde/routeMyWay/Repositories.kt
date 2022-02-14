package net.krupizde.routeMyWay

import org.hibernate.query.Query
import org.hibernate.transform.ResultTransformer
import org.hibernate.transform.Transformers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
@Repository
interface TripConnRep : JpaRepository<TripConnection, Int>{

}

@Repository
class TripConnectionRepository(@PersistenceContext private val entityManager: EntityManager, private val tripConnRep: TripConnRep) {
    fun insert(tripConnection: TripConnection) {
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

    fun selectAll(): List<TripConnection> {
        /*val query = entityManager.createNativeQuery("select depart.id as departId, depart.name as departName," +
                " depart.latitude as departLat, depart.longitude as departLong," +
                "arriv.id as arrivId, arriv.name as arrivName, arriv.latitude as arrivLat, arriv.longitude as arrivLong" +
                ", trip.id as tripId, trip.routeId as tripRouteId, " +
                "trip.serviceId as serviceId, trip.tripHeadSign as tripHeadSign, trip.tripShortName as tripShortName," +
                " departureTimeHour, departureTimeMinute, departureTimeSecond," +
                "arrivalTimeHour, arrivalTimeMinute, arrivalTimeSecond from tripConnection tripConn " +
                "inner join stop depart on depart.id = tripConn.departureStopId " +
                "inner join stop arriv on arriv.id = tripConn.arrivalStopId " +
                "inner join trip on trip.id = tripConn.tripId").unwrap(Query::class.java).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        *//*query.resultStream.map {
            if(it !is Map<*, *>)throw IllegalStateException("")
            return TripConnection()
        }.toList()*/
        return tripConnRep.findAll()
    }
}

@Repository
class StopRepository(@PersistenceContext private val entityManager: EntityManager) {
    fun insert(stop: Stop) {
        val query = entityManager.createNativeQuery(
            "INSERT INTO stop (stopId,name,latitude,longitude)" +
                    " VALUES (:id,:name,:latitude,:longitude)"
        )
        query.setParameter("id", stop.stopId)
        query.setParameter("name", stop.name)
        query.setParameter("latitude", stop.latitude)
        query.setParameter("longitude", stop.longitude)
        query.executeUpdate()
    }
}

@Repository
class TripRepository(@PersistenceContext private val entityManager: EntityManager) {
    fun insert(trip: Trip) {
        val query = entityManager.createNativeQuery(
            "INSERT INTO trip (tripId,routeId,serviceId,tripHeadSign,tripShortName)" +
                    " VALUES (:id,:routeId,:serviceId,:tripHeadSign,:tripShortName)"
        )
        query.setParameter("id", trip.tripId)
        query.setParameter("routeId", trip.routeId)
        query.setParameter("serviceId", trip.serviceId)
        query.setParameter("tripHeadSign", trip.tripHeadSign)
        query.setParameter("tripShortName", trip.tripShortName)
        query.executeUpdate()
    }
}