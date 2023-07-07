package net.krupizde.routeMyWay.domain.connections.obj

import kotlin.math.max

data class BasicStopProfile(val elements: MutableList<BasicStopProfileElement> = mutableListOf(BasicStopProfileElement())) {
    fun dominates(vector: BasicStopProfileElement): Boolean {
        val fromIndex = elements.indexOfFirst { it.departureTime >= vector.departureTime }
        for (element in elements.listIterator(fromIndex)) {
            if (vector.arrivalTime < element.arrivalTime) {
                break
            }
            if (element.dominates(vector)) {
                return true
            }
        }
        return false;
    }

    fun add(profile: BasicStopProfileElement) {
        if (dominates(profile)) {
            return
        };
        val index = max(elements.indexOfFirst { it.departureTime >= profile.departureTime }, 0)
        elements.add(index, profile)
        elements.subList(0, index).removeIf { profile.dominates(it) }
    }
}