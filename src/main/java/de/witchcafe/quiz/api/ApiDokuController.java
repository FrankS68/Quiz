package de.witchcafe.quiz.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.witchcafe.quiz.api.ApiAenderungen.Eintrag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Einstieg in die API-Doku (oeffentlich, ohne API-Key), analog knownet.
 * <ul>
 *   <li>{@code GET /api/docs} - was die API kann: Endpunkte, Rollen, Format, Fehlercodes, Version, Links</li>
 *   <li>{@code GET /api/docs/anleitung} - Anleitung mit Beispielen (Markdown)</li>
 *   <li>{@code GET /api/docs/aenderungen} - Aenderungsprotokoll, neueste zuerst</li>
 *   <li>{@code GET /api/docs/openapi.json} - vollstaendige OpenAPI-Beschreibung (springdoc)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/docs")
@Tag(name = "Doku", description = "API-Doku, Version und Aenderungsprotokoll (ohne API-Key)")
public class ApiDokuController {

    public record Endpunkt(String methode, String pfad, String rolle, String beschreibung) {}

    static final List<Endpunkt> ENDPUNKTE = List.of(
            new Endpunkt("GET", "/api/kategorien", "USER",
                    "Kategorienbaum: [{\"name\" (voller Pfad), \"oberkategorie\", \"anzahl\" (eigene Fragen), "
                            + "\"anzahlGesamt\" (inkl. Unterbereiche), \"schlagworte\"}]"),
            new Endpunkt("GET", "/api/fragen", "USER",
                    "Alle Fragen inkl. richtiger Antwort; ?kategorie=<pfad> filtert auf eine Kategorie inkl. ihrer "
                            + "Unterbereiche (Pfad wie in /api/kategorien). Format wie beim Import, zusaetzlich 'id'."),
            new Endpunkt("POST", "/api/fragen", "EDITOR",
                    "Fragenkatalog importieren, Body: JSON-Array von Fragen (Content-Type application/json). "
                            + "Antwort: {\"angelegt\", \"fehler\": [...]}"),
            new Endpunkt("POST", "/api/fragen", "EDITOR",
                    "Dasselbe als Datei-Upload: multipart/form-data, Feld 'file' mit dem JSON-Array"),
            new Endpunkt("GET", "/api/docs", "-", "Diese Uebersicht (ohne API-Key)"),
            new Endpunkt("GET", "/api/docs/anleitung", "-", "Anleitung mit Beispielen als Markdown (ohne API-Key)"),
            new Endpunkt("GET", "/api/docs/aenderungen", "-", "Aenderungsprotokoll der API (ohne API-Key)"),
            new Endpunkt("GET", "/api/docs/openapi.json", "-", "Vollstaendige OpenAPI-Beschreibung (ohne API-Key)"));

    @GetMapping
    @Operation(summary = "Was die API kann: Endpunkte, Rollen, Format, Fehlercodes, Version und Links",
            description = "Agenten rufen das zu Beginn eines Laufs auf und vergleichen 'version' mit der gemerkten. "
                    + "Ist sie neu, 'aenderungen' lesen.")
    public Map<String, Object> index() {
        Eintrag neuester = ApiAenderungen.PROTOKOLL.get(0);
        Map<String, Object> doku = new LinkedHashMap<>();
        doku.put("name", "Quiz API");
        doku.put("version", neuester.version());
        doku.put("stand", neuester.datum());
        doku.put("beschreibung", "Fragen und Kategorien des Quiz lesen und neue Fragenkataloge importieren. "
                + "Kategorien sind hierarchisch (Oberthema/Zertifikat/Abschnitt, getrennt durch '/') und haben "
                + "suchbare Schlagworte, die Editoren in der Oberflaeche pflegen. "
                + "Eine Frage hat ein Thema (= Kategorie), den Fragetext, Antwortmoeglichkeiten, die richtige Antwort "
                + "und optional eine Erklaerung und ein Bild. Quiz spielen und Ergebnisse ansehen geht nur in der Oberflaeche.");
        doku.put("authentifizierung", Map.of(
                "header", "Authorization: Bearer <API-Key>",
                "giltFuer", "alle /api/** ausser /api/docs/**",
                "keyBekommen", "Ein Admin legt den Key in der Oberflaeche unter Admin -> API-Keys (/admin/apikeys) an. "
                        + "Der Key beginnt mit 'qz_' und wird nur einmal angezeigt."));
        doku.put("rollen", Map.of(
                "USER", "nur lesen (Kategorien, Fragen)",
                "EDITOR", "lesen und Fragenkataloge importieren",
                "ADMIN", "wie EDITOR"));
        doku.put("endpunkte", ENDPUNKTE);
        doku.put("uploadDatei", Map.of(
                "format", "JSON-Datei mit einem Array von Fragen ([ {...}, {...} ]), auch bei nur einer Frage",
                "kodierung", "UTF-8",
                "maxGroesse", "10 MB",
                "giltFuer", "Upload in der Oberflaeche (Fragen -> Upload) und POST /api/fragen (Body oder Feld 'file')",
                "frage", "jedes Array-Element hat das Format aus 'frageFormat'; weitere Felder (z.B. 'id') werden ignoriert",
                "mehrereKategorien", "erlaubt; neue Kategorien entstehen automatisch aus 'thema'"));
        doku.put("frageFormat", Map.of(
                "thema", "Kategorie als Pfad, Ebenen mit '/' getrennt, z.B. \"Drohnen/EU A1-A3/Luftrecht\" (Pflicht)",
                "frage", "Fragetext (Pflicht)",
                "antworten", "Liste der Antwortmoeglichkeiten, die richtige eingeschlossen",
                "correct_answer", "die richtige Antwort, gleicher Text wie in 'antworten' (Pflicht)",
                "beschreibung", "optional: Erklaerung, max. 300 Zeichen",
                "bild", "optional: Bild zur Frage als http(s)-URL oder '/bilder/<id>' (hochgeladen in der Oberflaeche)",
                "beispiel", Map.of(
                        "thema", "Geografie/Deutschland/Fluesse",
                        "frage", "Welcher Fluss fliesst durch Koeln?",
                        "antworten", List.of("Main", "Rhein", "Elbe", "Mosel"),
                        "correct_answer", "Rhein",
                        "beschreibung", "Koeln liegt am Rhein.")));
        doku.put("fehlercodes", Map.of(
                "400", "ungueltiges JSON, Antwort {\"fehler\": \"...\"}",
                "401", "API-Key fehlt oder ist ungueltig",
                "403", "Rolle reicht nicht (Import nur EDITOR/ADMIN), Antwort {\"fehler\": \"...\"}"));
        doku.put("hinweise", List.of(
                "Texte als UTF-8 senden.",
                "Fehlerhafte Eintraege beim Import werden uebersprungen und in 'fehler' gemeldet, der Rest wird angelegt.",
                "Der Import prueft nicht auf Dubletten - vorher mit GET /api/fragen?kategorie=... nachsehen."));
        doku.put("anleitung", "/api/docs/anleitung");
        doku.put("aenderungen", "/api/docs/aenderungen");
        doku.put("openapi", "/api/docs/openapi.json");
        return doku;
    }

    @GetMapping("/aenderungen")
    @Operation(summary = "Aenderungsprotokoll der API, neueste zuerst")
    public List<Eintrag> aenderungen() {
        return ApiAenderungen.PROTOKOLL;
    }

    @GetMapping(value = "/anleitung", produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "Anleitung mit Beispielen (Markdown)")
    public ResponseEntity<String> anleitung() throws IOException {
        try (InputStream in = new ClassPathResource("api-doku/anleitung.md").getInputStream()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{version}}", ApiAenderungen.aktuelleVersion());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                    .body(text);
        }
    }
}
