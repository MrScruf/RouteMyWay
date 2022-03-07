package net.krupizde.routeMyWay

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("")
class Controller(val gtfs: Gtfs, val csa: CSA) {

    @PostMapping("/load")
    fun loadGtfs(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        gtfs.loadGtfsData(file.inputStream);
        return ResponseEntity.ok("Jdu");
    }

    @GetMapping("/{idStart}/{idStop}")
    fun test(@PathVariable idStart: String, @PathVariable idStop: String): ResponseEntity<*> {
        val out = csa.findShortestPathCSAProfile(idStart, idStop, Utils.generateTime(10, 10, 0))
        //println(out)
        return ResponseEntity.ok(out)
    }
}