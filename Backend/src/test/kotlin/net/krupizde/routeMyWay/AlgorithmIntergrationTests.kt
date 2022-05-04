package net.krupizde.routeMyWay

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.krupizde.routeMyWay.Business.CSA
import net.krupizde.routeMyWay.Business.DataProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import kotlin.math.ceil


@SpringBootTest
class AlgorithmIntergrationTests @Autowired constructor(
    private val csa: CSA,
    private val dataProvider: DataProvider,
    private val stopService: StopService,
    @org.springframework.beans.factory.annotation.Value("\${test.strictCompareWalking:false}") private val strictCompareWalking: Boolean,
    @org.springframework.beans.factory.annotation.Value("\${GOOGLE_API_KEY:none}") private val googleApiKey: String
) {
    @BeforeEach
    fun beforeEach() {
        dataProvider.reloadIfNotLoaded()
    }

    @Test
    fun `Find path between two random points and check them against Google DirectionsAPI`() = runBlocking {
        if (googleApiKey == "none") {
            return@runBlocking
        }
        val from = stopService.findByStopId("U321Z2P") ?: error("Starting stop not found")
        val to = stopService.findByStopId("U400S1") ?: error("Target stop not found")
        val zdt = ZonedDateTime.now()
        findAndComparePathsBetweenTwoStops(from, to, zdt)

    }

    private suspend fun findAndComparePathsBetweenTwoStops(from: Stop, to: Stop, zdt: ZonedDateTime = ZonedDateTime.now()) {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
        }
        val response: ResponseEntity = client.get("https://maps.googleapis.com/maps/api/directions/json") {
            parameter("origin", "${from.latitude},${from.longitude}")
            parameter("destination", "${to.latitude},${to.longitude}")
            parameter("key", googleApiKey)
            parameter("departure_time", ceil(zdt.toInstant().toEpochMilli() / 1000.0).toInt())
            parameter("mode", "transit")
        }
        val path = csa.findShortestPathCSAProfile(from.stopId, to.stopId, zdt.toLocalDateTime())
        val connections = path.firstOrNull()?.connections ?: Assertions.fail("Path not found")
        val googleLegs = response.routes.first().legs.first().steps
        val googleLegsIterator = googleLegs.listIterator()
        val myConnectionsIterator = connections.listIterator();
        //Discard initial footpaths
        if (myConnectionsIterator.hasNext() && myConnectionsIterator.next() is OutTripConnection) myConnectionsIterator.previous()
        if (myConnectionsIterator.hasNext() && googleLegsIterator.next().travelMode == TestUtils.TRAVEL_MODE_TRANSIT) myConnectionsIterator.previous()
        comparePathIterators(myConnectionsIterator, googleLegsIterator)
    }

    private fun comparePathIterators(
        myConnectionsIterator: ListIterator<Connection>,
        googleLegsIterator: ListIterator<ResponseEntityRouteStep>
    ) {
        while (googleLegsIterator.hasNext() && myConnectionsIterator.hasNext()) {
            val googleConnection = googleLegsIterator.next()
            val myConnection = myConnectionsIterator.next()
            assert(
                (googleConnection.travelMode == TestUtils.TRAVEL_MODE_WALKING && myConnection is OutFootConnection) ||
                        (googleConnection.travelMode == TestUtils.TRAVEL_MODE_TRANSIT && myConnection is OutTripConnection)
            ) {
                error("Compared path differs in traveling mode")
            }
            if (myConnection is OutFootConnection && strictCompareWalking) {
                assert(myConnection.durationInMinutes == ceil(googleConnection.duration.value / 60.0).toInt()) {
                    "Walking durations differ. Mine is ${myConnection.durationInMinutes} and google is ${googleConnection.duration.value}."
                }
            } else if (myConnection is OutTripConnection) {
                val googleTransitDetails = googleConnection.transitDetails ?: error("");
                assert(
                    (myConnection.departureStop.name == googleTransitDetails.departureStop.name &&
                            myConnection.arrivalStop.name == googleTransitDetails.arrivalStop.name)
                ) { "Departure or arrival stops dont match." } // TODO - možná přejít na porovnání zeměpisné šířky a délky ?
                val googleDepartureTime = TestUtils.timeFromSeconds(googleTransitDetails.departureTime.value)
                val googleArrivalTime = TestUtils.timeFromSeconds(googleTransitDetails.arrivalTime.value)
                assert(
                    myConnection.departureTime == googleDepartureTime &&
                            myConnection.arrivalTime == googleArrivalTime
                ) { "Departure or arrival times dont match." }
            }
        }
    }
}

