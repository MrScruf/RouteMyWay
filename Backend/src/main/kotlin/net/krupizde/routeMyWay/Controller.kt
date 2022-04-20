package net.krupizde.routeMyWay

import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import javax.servlet.http.HttpServletResponse
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping("")
class Controller(
    private val gtfs: Gtfs,
    private val csa: CSA,
    private val stopService: StopService,
    private val csvWriter: CsvWriter,
    private val routeTypeService: RouteTypeService
) {

    @PostMapping("/load")
    fun loadGtfs(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        gtfs.loadGtfsData(file.inputStream);
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
    @GetMapping("/path/gtfs")
    fun pathGtfs(
        @NotBlank @RequestParam departureStopId: String,
        @NotBlank @RequestParam arrivalStopId: String,
        @RequestParam(required = false) departureTime: String?,
        @RequestParam(required = false, defaultValue = "false") bikesAllowed: Boolean,
        @RequestParam(required = false, defaultValue = "false") wheelChairsAllowed: Boolean,
        @RequestParam(required = false) vehiclesAllowed: Set<Int>?,
         response : HttpServletResponse
    ): ResponseEntity<*> {
        val time = if (departureTime != null) LocalDateTime.parse(departureTime) else LocalDateTime.now()
        val out = csa.findShortestPathCSAProfileGtfs(
            departureStopId, arrivalStopId, time, bikesAllowed, wheelChairsAllowed, vehiclesAllowed
        )
        return ResponseEntity.ok()
            .header(CONTENT_DISPOSITION, "attachment;filename=gtfs.zip")
            .contentType(MediaType.valueOf("application/zip"))
            .body(csvWriter.writeGtfsPathToZip(out).toByteArray())
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