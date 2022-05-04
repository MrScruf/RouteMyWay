package net.krupizde.routeMyWay.Presentation

import net.krupizde.routeMyWay.Business.CSA
import net.krupizde.routeMyWay.Business.Gtfs
import net.krupizde.routeMyWay.RouteType
import net.krupizde.routeMyWay.RouteTypeService
import net.krupizde.routeMyWay.Stop
import net.krupizde.routeMyWay.StopService
import net.krupizde.routeMyWay.Utils.ThreadSemaphore
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("")
class Controller(
    private val gtfs: Gtfs,
    private val csa: CSA,
    private val stopService: StopService,
    private val routeTypeService: RouteTypeService,
    private val threadSemaphore: ThreadSemaphore,
    @Value("\${updatePassword:admin}") private val updatePassword: String
) {

    @PostMapping("/load")
    fun loadGtfs(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("password") password: String
    ): ResponseEntity<*> {
        if (password != updatePassword) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        if (!gtfs.loadGtfsData(file.inputStream)) ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Already running")
        return ResponseEntity.ok("Updated");
    }

    @GetMapping("/path/json")
    fun pathJson(
        @NotBlank @RequestParam departureStopId: String,
        @NotBlank @RequestParam arrivalStopId: String,
        @RequestParam(required = false) departureTime: String?,
        @RequestParam(required = false, defaultValue = "1") numberOfPaths: Int,
        @RequestParam(required = false, defaultValue = "false") bikesAllowed: Boolean,
        @RequestParam(required = false, defaultValue = "false") wheelChairsAllowed: Boolean,
        @RequestParam(required = false) vehiclesAllowed: Set<Int>?
    ): ResponseEntity<*> {
        val time = if (departureTime != null) LocalDateTime.parse(departureTime) else LocalDateTime.now()
        val out = csa.findShortestPathCSAProfile(
            departureStopId, arrivalStopId, time, bikesAllowed, wheelChairsAllowed, vehiclesAllowed, numberOfPaths
        )
        return ResponseEntity.ok(out)
    }

    @GetMapping("/stops")
    fun stops(@RequestParam name: String): List<Stop> {
        return stopService.findAllByName(name)
    }

    @GetMapping("/vehicles")
    fun vehicleTypes(): List<RouteType> {
        return routeTypeService.findAll()
    }
}