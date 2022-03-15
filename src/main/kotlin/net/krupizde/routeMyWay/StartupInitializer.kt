package net.krupizde.routeMyWay

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupInitializer @Autowired constructor(val dataCache: DataCache) {

    @EventListener(ApplicationStartedEvent::class)
    fun doSomethingAfterStartup() {
        dataCache.reloadData()
    }
}