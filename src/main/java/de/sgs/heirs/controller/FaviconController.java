package de.sgs.heirs.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {

    @GetMapping("favicon.ico")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Gibt HTTP 204 zurück
    public void ignoreFavicon() {
        // Macht nichts – verhindert nur den 404-Fehler im Browser/Log
    }
}