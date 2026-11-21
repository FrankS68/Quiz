package de.witchcafe.quiz.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Einstieg in die API-Doku (oeffentlich, ohne API-Key). */
@RestController
@RequestMapping("/api/docs")
@Tag(name = "Doku", description = "API-Version und Links (ohne API-Key)")
public class ApiDokuController {

    /** Bei jeder Aenderung der API hochzaehlen. */
    static final String VERSION = "1.0.0";

    @GetMapping
    @Operation(summary = "Aktuelle API-Version und Links zur Doku")
    public Map<String, Object> index() {
        return Map.of(
                "version", VERSION,
                "openapi", "/api/docs/openapi.json",
                "authentifizierung", "Header 'Authorization: Bearer <API-Key>' fuer alle /api/** ausser /api/docs",
                "importFormat", "JSON-Array wie beim Upload: [{\"thema\",\"frage\",\"antworten\":[...],\"correct_answer\",\"beschreibung\"?}]");
    }
}
