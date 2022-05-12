package net.krupizde.routeMyWay

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.krupizde.routeMyWay.TestUtils.compareStops
import net.krupizde.routeMyWay.business.CSA
import net.krupizde.routeMyWay.business.DataProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import kotlin.math.ceil


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
class AlgorithmIntegrationTests @Autowired constructor(
    private val csa: CSA,
    private val dataProvider: DataProvider,
    private val stopService: StopService,
    @org.springframework.beans.factory.annotation.Value("\${test.strictCompareWalking:false}") private val strictCompareWalking: Boolean,
    @org.springframework.beans.factory.annotation.Value("\${GOOGLE_API_KEY:none}") private val googleApiKey: String
) {
    private val logger: Logger = LoggerFactory.getLogger(AlgorithmIntegrationTests::class.java)

    @BeforeEach
    fun beforeEach() {
        dataProvider.reloadIfNotLoaded()
    }

    fun stopIds(): Array<Pair<String, String>> {
        return arrayOf(Pair("U1384Z301", "U1593Z301"), Pair("U1384Z301", "U2931Z301"), Pair("U1384Z301", "U142Z1P"))
    }

    @MethodSource(value = ["stopIds"])
    @org.junit.jupiter.params.ParameterizedTest
    fun `Find path between two points and check them against Google DirectionsAPI`(stops: Pair<String, String>) =
        runBlocking {
            if (googleApiKey == "none") {
                return@runBlocking
            }
            val from = stopService.findByStopId(stops.first) ?: error("Starting stop not found")
            val to = stopService.findByStopId(stops.second) ?: error("Target stop not found")
            val zdt = ZonedDateTime.now().withHour(7)
            findAndComparePathsBetweenTwoStops(from, to, zdt)
        }

    private suspend fun findAndComparePathsBetweenTwoStops(
        from: Stop,
        to: Stop,
        zdt: ZonedDateTime = ZonedDateTime.now()
    ) {
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
            parameter("transit_routing_preference", "less_walking")
        }
        val path = csa.findShortestPathCSAProfile(from.stopId, to.stopId, zdt.toLocalDateTime())
        val connections = path.firstOrNull()?.connections ?: Assertions.fail("Path not found")
        val googleLegs = response.routes.first().legs.first().steps
        val googleLegsIterator = googleLegs.listIterator()
        val myConnectionsIterator = connections.listIterator();
        //Discard initial footpaths
        if (myConnectionsIterator.hasNext() && myConnectionsIterator.next() is OutTripConnection) {
            myConnectionsIterator.previous()
        }
        if (googleLegsIterator.hasNext() && googleLegsIterator.next().travelMode == TestUtils.TRAVEL_MODE_TRANSIT) {
            googleLegsIterator.previous()
        }
        comparePathIterators(myConnectionsIterator, googleLegsIterator)
    }

    private fun comparePathIterators(
        myConnectionsIterator: ListIterator<Connection>,
        googleLegsIterator: ListIterator<ResponseEntityRouteStep>
    ) {
        while (googleLegsIterator.hasNext() && myConnectionsIterator.hasNext()) {
            var googleConnection = googleLegsIterator.next()
            var myConnection = myConnectionsIterator.next()
            if (myConnection is OutFootConnection && myConnectionsIterator.hasNext()) {
                myConnection = myConnectionsIterator.next()
            }
            if (googleConnection.travelMode == TestUtils.TRAVEL_MODE_WALKING && googleLegsIterator.hasNext()) {
                googleConnection = googleLegsIterator.next()
            }
            if(googleConnection.travelMode == TestUtils.TRAVEL_MODE_WALKING && myConnection is OutFootConnection){
                continue
            }
            val googleTransitDetails = googleConnection.transitDetails ?: error("");
            assert(
                compareStops(myConnection, googleTransitDetails)
            ) { "Departure or arrival stops dont match." }
            val googleDepartureTime = TestUtils.timeFromSeconds(googleTransitDetails.departureTime.value)
            val googleArrivalTime = TestUtils.timeFromSeconds(googleTransitDetails.arrivalTime.value)
            val tripConnection = myConnection as OutTripConnection
            assert(
                tripConnection.departureTime == googleDepartureTime &&
                        tripConnection.arrivalTime == googleArrivalTime
            ) { "Departure or arrival times dont match." }
        }
    }
}

