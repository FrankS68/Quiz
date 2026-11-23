package de.witchcafe.quiz.ki;

import java.util.List;
import java.util.Map;

/**
 * Ein KI-Anbieter (Claude, Mistral, OpenAI, Gemini ...). Neue Anbieter sind einfach eine weitere
 * Spring-Bean, die dieses Interface umsetzt; in {@code quiz.ki.modelle} werden ihre Modelle als
 * {@code <id>:<modell>} eingetragen.
 */
public interface KiAnbieter {

    /** Kurzname in quiz.ki.modelle, z. B. "anthropic". */
    String id();

    /** Anzeigename im Dialog. */
    String name();

    /** API-Key gesetzt? Sonst erscheinen die Modelle nicht. */
    boolean konfiguriert();

    /**
     * Erzeugt die Antwort als JSON-Text nach {@code schema}.
     * Kann der Anbieter Links nicht selbst lesen, holt er sie ueber {@link DokumentLeser}.
     *
     * @param hinweise hier landen Hinweise fuer den Redakteur (z. B. nicht lesbarer Link)
     */
    String erzeuge(Anfrage anfrage, List<String> hinweise);

    record Anfrage(String modell, String system, String aufgabe, List<String> links, Map<String, Object> schema) {
    }
}
