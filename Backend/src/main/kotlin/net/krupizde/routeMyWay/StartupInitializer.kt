package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupInitializer @Autowired constructor(val dataProvider: DataProvider) {

    @EventListener(ApplicationStartedEvent::class)
    fun doSomethingAfterStartup() {
        dataProvider.reloadData()
    }
}