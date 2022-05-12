package net.krupizde.routeMyWay.utils

import net.krupizde.routeMyWay.business.DataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupInitializer @Autowired constructor(val dataProvider: DataProvider) {

    @EventListener(ContextRefreshedEvent::class)
    fun doSomethingAfterStartup() {
        dataProvider.reloadData()
    }
}