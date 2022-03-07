package net.krupizde.routeMyWay

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DataHolder(private val tripConnectionsService: TripConnectionsService, private var data: List<TripConnection>? = null) {
    fun getData(): List<TripConnection> {
        if (data == null) {
            data = tripConnectionsService.findAll()
        }
        return data ?: throw IllegalStateException("No data loaded")
    }
}