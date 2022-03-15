package net.krupizde.routeMyWay

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("")
class Controller(private val gtfs: Gtfs, private val csa: CSA) {

    @PostMapping("/load")
    fun loadGtfs(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        gtfs.loadGtfsData(file.inputStream);
        return ResponseEntity.ok("Jdu");
    }

    @GetMapping("/{departureStopId}/{arrivalStopId}")
    fun test(
        @PathVariable departureStopId: String,
        @PathVariable arrivalStopId: String,
        @RequestBody time: Time
    ): ResponseEntity<*> {
        //TODO - najdi vysledek csa.query pro muzeum-A na Haje
        val out = csa.findShortestPathCSAProfile(departureStopId, arrivalStopId, Utils.generateTime(time))
        //println(out)
        return ResponseEntity.ok(out)
    }
}