package net.krupizde.routeMyWay

class TestUtils {
    companion object {
        private const val TRAVEL_MODE_WALKING = "WALKING"
        fun summarizePathways(steps: List<ResponseEntityRouteStep>){
            var step: ResponseEntityRouteStep = steps.first();
            var index = 0
            while(step.travelMode == TRAVEL_MODE_WALKING){

            }
        }
    }
}