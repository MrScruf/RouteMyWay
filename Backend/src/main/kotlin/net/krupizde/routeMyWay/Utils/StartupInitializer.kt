package net.krupizde.routeMyWay.Utils

import net.krupizde.routeMyWay.Business.DataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupInitializer @Autowired constructor(val dataProvider: DataProvider) {

    fun isJUnitTest(): Boolean {
        for (element in Thread.currentThread().stackTrace) {
            if (element.className.startsWith("org.junit.")) {
                return true
            }
        }
        return false
    }

    @EventListener(ContextRefreshedEvent::class)
    fun doSomethingAfterStartup() {
        if(isJUnitTest())return
        dataProvider.reloadData()
    }
}