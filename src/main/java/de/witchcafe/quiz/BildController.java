package de.witchcafe.quiz;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liefert hochgeladene Fragen-Bilder aus. Liegt hinter der witch-auth-Kette, d.h. nur fuer
 * angemeldete Benutzer (wie das Quiz selbst). Bilder aendern sich nie (neues Bild = neue ID),
 * deshalb lange cachebar.
 */
@RestController
public class BildController {

    public static final String PFAD = "/bilder/";

    private final BildRepository bildRepository;

    BildController(BildRepository bildRepository) {
        this.bildRepository = bildRepository;
    }

    @GetMapping(PFAD + "{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> bild(@PathVariable long id) {
        return bildRepository.findById(id)
                .map(b -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(b.getContentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate().immutable())
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(b.getDaten()))
                .orElse(ResponseEntity.notFound().build());
    }
}
