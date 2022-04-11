package net.krupizde.routeMyWay

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest

class AlgorithmIntergrationTests @Autowired constructor(
    private val csa: CSA,
    @Value("\${GOOGLE_API_KEY}") private val googleApiKey: String
) {
    @Test
    fun `Find path between two random points and check them against Google DirectionsAPI`() = runBlocking {
        val from = Stop("U306Z101P", "Nemocnice Motol", null, null, LocationType(1, ""), null)
        val to = Stop("U286Z101P", "Háje", null, null, LocationType(1, ""), null)

        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
        }
        val response: ResponseEntity = client.get("https://maps.googleapis.com/maps/api/directions/json") {
            parameter("origin", from.name)
            parameter("destination", to.name)
            parameter("key", googleApiKey)
            parameter("departure_time", "1647244800")
            parameter("mode", "transit")
        }
        //val path = csa.findShortestPathCSAProfile(from.stopId, to.stopId, LocalDateTime.now())

    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntity(@JsonAlias("routes") val routes: List<ResponseEntityRoute>);
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntityRoute(@JsonAlias("legs") val legs: List<ResponseEntityRouteLeg>);
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntityRouteLeg(@JsonAlias("steps") val steps: List<ResponseEntityRouteStep>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseEntityRouteStep(
    @JsonAlias("travel_mode") val travelMode: String,
    @JsonAlias("duration") val duration: Value,
    @JsonAlias("distance") val distance: Value,
    @JsonAlias("transit_details") val transitDetails: TransitDetails?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitDetails(
    @JsonAlias("arrival_stop") val arrivalStop: TransitDetailsStop,
    @JsonAlias("departure_stop") val departureStop: TransitDetailsStop,
    @JsonAlias("arrival_time") val arrivalTime: Value,
    @JsonAlias("departure_time") val departureTime: Value,
    @JsonAlias("num_stops") val numberOfStops: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitDetailsStop(val name: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Value(val value: Int)
