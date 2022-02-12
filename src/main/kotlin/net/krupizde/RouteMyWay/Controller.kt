package net.krupizde.RouteMyWay

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("")
class Controller(val gtfs: Gtfs) {

    @PostMapping("")
    fun loadGtfs(@RequestParam("file") file: MultipartFile): ResponseEntity<*>{
        gtfs.load(file.inputStream);
        return ResponseEntity.ok("Jdu");
    }
}