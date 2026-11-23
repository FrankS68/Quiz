package de.witchcafe.quiz;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Eine Frage im Import-/Exportformat (dasselbe JSON wie beim Upload in der Oberflaeche):
 * {@code {"thema": "...", "frage": "...", "antworten": ["..."], "correct_answer": "...", "beschreibung": "...", "bild": "https://..."}}.
 * Weitere Felder (z.B. "id") werden ignoriert.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FrageImport(
        String thema,
        String frage,
        List<String> antworten,
        @JsonProperty("correct_answer") String correctAnswer,
        String beschreibung,
        String bild) {
}
