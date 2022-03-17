package net.krupizde.routeMyWay

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("")
class Controller(private val gtfs: Gtfs, private val csa: CSA, private val stopService: StopService) {

    @PostMapping("/load")
    fun loadGtfs(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        gtfs.loadGtfsData(file.inputStream);
        return ResponseEntity.ok("Jdu");
    }

    @GetMapping("/{departureStopId}/{arrivalStopId}")
    fun test(
        @PathVariable departureStopId: String, @PathVariable arrivalStopId: String, @RequestParam departureTime: String
    ): ResponseEntity<*> {
        val out = csa.findShortestPathCSAProfile(departureStopId, arrivalStopId, Utils.stringToTime(departureTime))
        //println(out)
        return ResponseEntity.ok(out)
    }

    @GetMapping("/stops")
    fun stops(): List<Stop> {
        return stopService.findAll()
    }
}