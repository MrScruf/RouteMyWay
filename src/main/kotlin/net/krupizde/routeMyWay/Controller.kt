package net.krupizde.routeMyWay

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@RestController
@RequestMapping("")
class Controller(private val gtfs: Gtfs, private val csa: CSA, private val stopService: StopService) {

    @PostMapping("/load")
    fun loadGtfs(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        gtfs.loadGtfsData(file.inputStream);
        return ResponseEntity.ok("Done");
    }

    @GetMapping("/{departureStopId}/{arrivalStopId}")
    fun test(
        @PathVariable departureStopId: String, @PathVariable arrivalStopId: String,
        @RequestParam departureTime: String?
    ): ResponseEntity<*> {
        val time = if (departureTime != null) LocalDateTime.parse(departureTime) else LocalDateTime.now()
        val out = csa.findShortestPathCSAProfile(departureStopId, arrivalStopId, time)
        return ResponseEntity.ok(out)
    }

    @GetMapping("/stops")
    fun stops(@RequestParam name: String): List<Stop> {
        return stopService.findAllByName(name)
    }
}