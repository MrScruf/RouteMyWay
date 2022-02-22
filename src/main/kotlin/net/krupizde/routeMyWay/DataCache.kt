package net.krupizde.routeMyWay

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class DataCache(
    private val tripConnectionsService: TripConnectionsService,
    private val footConnectionsService: FootConnectionsService
) {
    private var tripConnections: List<TripConnection>? = null;
    private var footConnections: Map<String, List<FootConnection>>? = null;
    fun reloadData(){
        tripConnections = tripConnectionsService.findAll()
        val tmp = footConnectionsService.findAll()
        val tmpMap = mutableMapOf<String, MutableList<FootConnection>>()
        for (footConnection in tmp) {
            tmpMap.getOrPut(footConnection.arrivalStopId) { mutableListOf() }.add(footConnection)
        }
        footConnections = tmpMap.toMap()
    }
    fun getData(): Pair<List<TripConnection>, Map<String, List<FootConnection>>> {
        if (tripConnections == null) {
            reloadData()
        }
        return Pair(
            tripConnections ?: throw IllegalStateException("No data loaded"),
            footConnections ?: throw IllegalStateException("No data loaded")
        )
    }
}