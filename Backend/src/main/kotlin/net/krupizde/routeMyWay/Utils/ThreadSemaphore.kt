package net.krupizde.routeMyWay.Utils

import kotlinx.coroutines.sync.Semaphore
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ThreadSemaphore(val value: Semaphore = Semaphore(1)) {
}