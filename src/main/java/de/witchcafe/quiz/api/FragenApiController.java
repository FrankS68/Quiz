package de.witchcafe.quiz.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.witchcafe.quiz.FrageImport;
import de.witchcafe.quiz.KategorieService;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizItemService.ImportErgebnis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Fragen lesen und Fragenkataloge importieren. Lesen darf jeder gueltige API-Key,
 * Importieren nur Keys mit Rolle EDITOR oder ADMIN (@PreAuthorize im QuizItemService).
 */
@RestController
@Tag(name = "Fragen", description = "Fragen und Kategorien lesen, Fragenkataloge importieren")
public class FragenApiController {

    private final QuizItemService quizItemService;
    private final KategorieService kategorieService;
    private final ObjectMapper objectMapper;

    public FragenApiController(QuizItemService quizItemService, KategorieService kategorieService,
            ObjectMapper objectMapper) {
        this.quizItemService = quizItemService;
        this.kategorieService = kategorieService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/kategorien")
    @Operation(summary = "Alle Kategorien als Baum-Knoten (Pfad mit '/'), mit Anzahl der Fragen und Schlagworten",
            description = "Enthaelt auch Oberthemen ohne eigene Fragen. 'anzahl' = Fragen genau dieser Kategorie, "
                    + "'anzahlGesamt' = inkl. aller Unterbereiche.")
    public List<Kategorie> kategorien() {
        return kategorieService.alle().stream()
                .map(i -> new Kategorie(i.pfad(), i.eltern(), i.eigeneFragen(), i.fragenGesamt(), i.schlagworte()))
                .toList();
    }

    @GetMapping("/api/fragen")
    @Operation(summary = "Fragen, optional nur einer Kategorie inkl. ihrer Unterbereiche",
            description = "Gleiches Format wie der Import, zusaetzlich mit 'id'.")
    public List<Frage> fragen(@RequestParam(required = false) String kategorie) {
        List<QuizItem> items = kategorie == null || kategorie.isBlank()
                ? quizItemService.findAll()
                : quizItemService.findImTeilbaum(de.witchcafe.quiz.Kategorie.normalisiere(kategorie));
        return items.stream().map(Frage::von).toList();
    }

    @PostMapping(value = "/api/fragen", consumes = "application/json")
    @Operation(summary = "Fragenkatalog als JSON-Body importieren (Rolle EDITOR oder ADMIN)",
            description = "JSON-Array wie beim Upload: thema, frage, antworten, correct_answer, optional beschreibung.")
    public ImportErgebnis importJson(@RequestBody List<FrageImport> fragen) {
        return quizItemService.importiere(fragen);
    }

    @PostMapping(value = "/api/fragen", consumes = "multipart/form-data")
    @Operation(summary = "Fragenkatalog als Datei importieren (Feld 'file', gleiches JSON-Format)")
    public ResponseEntity<ImportErgebnis> importDatei(@RequestParam("file") MultipartFile file) {
        List<FrageImport> fragen;
        try {
            fragen = objectMapper.readValue(file.getInputStream(), new TypeReference<List<FrageImport>>() { });
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ImportErgebnis(0, List.of("Datei ist kein gueltiges JSON-Array: " + e.getMessage())));
        }
        return ResponseEntity.ok(quizItemService.importiere(fragen));
    }

    /** {@code name} ist der volle Pfad (wie 'thema' beim Import), {@code oberkategorie} der Pfad darueber. */
    public record Kategorie(String name, String oberkategorie, long anzahl, long anzahlGesamt, String schlagworte) {
    }

    /** Frage im Exportformat (Feldnamen wie beim Import). */
    public record Frage(
            Long id,
            String thema,
            String frage,
            List<String> antworten,
            @com.fasterxml.jackson.annotation.JsonProperty("correct_answer") String correctAnswer,
            String beschreibung) {

        static Frage von(QuizItem q) {
            return new Frage(q.getId(), q.getCategory(), q.getQuestion(), q.getOptions(), q.getAnswer(),
                    q.getDescription());
        }
    }
}
