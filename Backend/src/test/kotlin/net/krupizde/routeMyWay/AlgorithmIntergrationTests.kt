package net.krupizde.routeMyWay

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime


@SpringBootTest
class AlgorithmIntergrationTests @Autowired constructor(
    private val csa: CSA,
    private val dataProvider: DataProvider,
    @org.springframework.beans.factory.annotation.Value("\${GOOGLE_API_KEY}") private val googleApiKey: String
) {
    @BeforeEach
    fun beforeEach() {
        dataProvider.reloadIfNotLoaded()
    }

    @Test
    fun `Find path between two random points and check them against Google DirectionsAPI`() = runBlocking {
        val from = Stop("U306Z101P", "Nemocnice Motol", null, null, LocationType(1, ""), null)
        val to = Stop("U286Z101P", "Háje", null, null, LocationType(1, ""), null)
        val zdt = ZonedDateTime.now()

        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
        }
        val response: ResponseEntity = client.get("https://maps.googleapis.com/maps/api/directions/json") {
            parameter("origin", from.name)
            parameter("destination", to.name)
            parameter("key", googleApiKey)
            parameter("departure_time", zdt.toInstant().toEpochMilli())
            parameter("mode", "transit")
        }
        val path = csa.findShortestPathCSAProfile(from.stopId, to.stopId, zdt.toLocalDateTime())
        val connections = path.paths.first()
        val googleLegs = response.routes.first().legs.first()
        
    }
}

