package de.witchcafe.quiz.ki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import de.witchcafe.quiz.FrageImport;
import de.witchcafe.quiz.Kategorie;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizItemService.ImportErgebnis;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Laesst eine KI (Claude, Mistral, OpenAI, Gemini ... siehe {@link KiAnbieter}) Fragen fuer eine neue oder bestehende Kategorie erzeugen.
 * Die Fragen kommen als Vorschlag zurueck; gespeichert wird erst mit {@link #importiere}, und zwar
 * ueber denselben Weg wie Upload und REST-API ({@link QuizItemService#importiere}).
 *
 * Konfiguration: quiz.ki.modelle = Liste "anbieter:modell"; je Anbieter ein Key
 * (quiz.ki.anthropic-api-key, quiz.ki.mistral-api-key, quiz.ki.openai-api-key, quiz.ki.gemini-api-key),
 * Secrets nur per Umgebung oder application-local.properties. Ein Modell erscheint nur, wenn der Key
 * seines Anbieters gesetzt ist.
 */
@Service
public class KiFragenService {

    private static final Logger log = LoggerFactory.getLogger(KiFragenService.class);

    public static final int MAX_FRAGEN = 50;
    /** Bestehende Fragen, die hoechstens in den Prompt kommen (gegen Dubletten). */
    private static final int MAX_BESTEHENDE = 400;

    private final QuizItemService quizItemService;
    private final List<KiAnbieter> anbieter;
    private final List<String> modelle;
    private final JsonMapper json = JsonMapper.builder().build();

    KiFragenService(QuizItemService quizItemService, List<KiAnbieter> anbieter,
            @Value("${quiz.ki.modelle:}") String modelle) {
        this.quizItemService = quizItemService;
        this.anbieter = anbieter;
        this.modelle = Arrays.stream(modelle.split(",")).map(String::trim).filter(s -> s.contains(":")).toList();
    }

    /** Ein waehlbares Modell: Eintrag "anbieter:modell" aus quiz.ki.modelle. */
    public record Modell(String eintrag, KiAnbieter anbieter, String name) {
        public String anzeige() {
            return anbieter.name() + " · " + name;
        }
    }

    /** Wuensche aus dem Dialog. */
    public record Auftrag(String kategorie, String idee, List<String> links, int anzahl, String modell) {
    }

    /** Vorschlaege der KI plus Hinweise (z. B. nicht lesbare Links). */
    public record Vorschlag(List<KiFrage> fragen, List<String> hinweise) {
    }

    /** Modelle, fuer deren Anbieter ein Key konfiguriert ist (leer = Funktion aus). Liefert die Eintraege. */
    public List<String> verfuegbareModelle() {
        return modelle.stream().filter(e -> {
            Modell m = finde(e);
            return m != null && m.anbieter().konfiguriert();
        }).toList();
    }

    /** Anzeigename eines Eintrags fuer den Dialog. */
    public String anzeige(String eintrag) {
        Modell m = finde(eintrag);
        return m == null ? eintrag : m.anzeige();
    }

    public boolean aktiv() {
        return !verfuegbareModelle().isEmpty();
    }

    private Modell finde(String eintrag) {
        if (eintrag == null || !eintrag.contains(":")) {
            return null;
        }
        String id = eintrag.substring(0, eintrag.indexOf(':'));
        String name = eintrag.substring(eintrag.indexOf(':') + 1);
        return anbieter.stream().filter(an -> an.id().equals(id)).findFirst()
                .map(an -> new Modell(eintrag, an, name)).orElse(null);
    }

    private Modell modell(String eintrag) {
        Modell m = finde(eintrag);
        if (m == null || !m.anbieter().konfiguriert() || !modelle.contains(eintrag)) {
            throw new KiFehler("Modell " + eintrag + " ist nicht konfiguriert");
        }
        return m;
    }

    /** Fragen der Kategorie, die die KI kennen soll (nur genau diese Kategorie). */
    public List<QuizItem> bestehendeFragen(String kategorie) {
        return quizItemService.findByCategory(Kategorie.normalisiere(kategorie));
    }

    /**
     * Ruft die KI auf (dauert je nach Modell und Dokumenten bis zu einigen Minuten).
     * Braucht keinen angemeldeten Benutzer und laeuft deshalb auch im Hintergrund-Thread;
     * die bestehenden Fragen holt der Aufrufer vorher.
     */
    public Vorschlag erzeuge(Auftrag auftrag, List<QuizItem> bestehende) {
        Modell m = modell(auftrag.modell());
        List<String> hinweise = new ArrayList<>();
        String text = m.anbieter().erzeuge(new KiAnbieter.Anfrage(m.name(), systemPrompt(),
                aufgabe(auftrag, bestehende), auftrag.links(), schema()), hinweise);
        Vorschlag v = parse(text);
        hinweise.addAll(v.hinweise());
        return bereinige(new Vorschlag(v.fragen(), hinweise), bestehende);
    }

    /** Speichert die ausgewaehlten Fragen in der Kategorie (braucht Rolle EDITOR oder ADMIN). */
    public ImportErgebnis importiere(String kategorie, List<KiFrage> fragen) {
        List<Map<String, Object>> roh = new ArrayList<>();
        for (KiFrage f : fragen) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("thema", Kategorie.normalisiere(kategorie));
            m.put("frage", f.frage());
            m.put("antworten", f.antworten());
            m.put("correct_answer", f.correctAnswer());
            m.put("beschreibung", f.beschreibung());
            roh.add(m);
        }
        List<FrageImport> importe = json.convertValue(roh, new TypeReference<List<FrageImport>>() {
        });
        return quizItemService.importiere(importe);
    }

    // ---------------------------------------------------------------- Prompt

    private static String systemPrompt() {
        return """
                Du erstellst Fragen fuer ein Lern-Quiz. Jede Frage wird auf drei Arten gespielt: \
                als Multiple Choice (die Antwortoptionen werden gemischt angezeigt), als Selbsttest \
                (die richtige Antwort wird aufgedeckt) und als Lueckentext (Stichworte der richtigen \
                Antwort muessen eingetippt werden).

                Regeln fuer jede Frage:
                - "frage": eine klare, eigenstaendig verstaendliche Frage, ohne Bezug wie "laut Text".
                - "antworten": genau drei Antwortoptionen; eine davon ist die richtige, Wort fuer Wort \
                identisch mit "correct_answer". Die zwei falschen sind plausibel, aehnlich lang und \
                eindeutig falsch. Keine Optionen wie "alle genannten" oder "keine davon".
                - "correct_answer": die richtige Antwort, knapp, mit den entscheidenden Stichworten \
                (Zahlen, Fachbegriffe), damit der Lueckentext funktioniert.
                - "beschreibung": kurze Erklaerung, warum die Antwort stimmt, hoechstens %d Zeichen.
                - Sprache: Deutsch, sofern der Auftrag nichts anderes sagt.
                - Fachlich korrekt. Sind Dokumente angegeben, fragst du deren Stoff ab und erfindest \
                nichts, was dort nicht steht. Ohne Dokumente nur gesichertes Wissen.
                - Keine Dubletten: weder untereinander noch zu den bereits vorhandenen Fragen \
                (auch nicht dieselbe Frage anders formuliert).
                - Decke das Thema breit ab statt viele Varianten derselben Tatsache.

                In "hinweis" schreibst du in einem Satz, was der Redakteur wissen sollte \
                (z. B. ein Dokument war nicht lesbar, oder es gab weniger Stoff als gewuenschte Fragen); \
                sonst einen leeren Text.
                """.formatted(QuizItem.DESCRIPTION_MAX_LENGTH);
    }

    private static String aufgabe(Auftrag a, List<QuizItem> bestehende) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kategorie (Pfad, Ebenen mit /): ").append(Kategorie.normalisiere(a.kategorie())).append("\n\n");
        sb.append("Idee bzw. gewuenschte Erweiterung:\n").append(a.idee() == null || a.idee().isBlank()
                ? "(keine Beschreibung, leite das Thema aus der Kategorie ab)" : a.idee().trim()).append("\n\n");
        sb.append("Anzahl neuer Fragen: ").append(a.anzahl()).append("\n\n");
        if (!a.links().isEmpty()) {
            sb.append("Dokumente, deren Stoff abgefragt werden soll:\n");
            a.links().forEach(l -> sb.append("- ").append(l).append('\n'));
            sb.append('\n');
        }
        if (bestehende.isEmpty()) {
            sb.append("Die Kategorie hat noch keine Fragen.\n");
        } else {
            sb.append("Bereits vorhandene Fragen dieser Kategorie (nicht wiederholen):\n");
            bestehende.stream().limit(MAX_BESTEHENDE)
                    .forEach(q -> sb.append("- ").append(q.getQuestion()).append(" -> ").append(q.getAnswer()).append('\n'));
            if (bestehende.size() > MAX_BESTEHENDE) {
                sb.append("(und ").append(bestehende.size() - MAX_BESTEHENDE).append(" weitere)\n");
            }
        }
        return sb.toString();
    }

    /** JSON-Schema der Antwort, fuer Claude und Mistral gleich. */
    private static Map<String, Object> schema() {
        Map<String, Object> frage = Map.of(
                "type", "object",
                "properties", Map.of(
                        "frage", Map.of("type", "string"),
                        "antworten", Map.of("type", "array", "items", Map.of("type", "string")),
                        "correct_answer", Map.of("type", "string"),
                        "beschreibung", Map.of("type", "string")),
                "required", List.of("frage", "antworten", "correct_answer", "beschreibung"),
                "additionalProperties", false);
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "fragen", Map.of("type", "array", "items", frage),
                        "hinweis", Map.of("type", "string")),
                "required", List.of("fragen", "hinweis"),
                "additionalProperties", false);
    }

    // ---------------------------------------------------------------- Auswertung

    private Vorschlag parse(String text) {
        JsonNode wurzel;
        try {
            wurzel = json.readTree(text);
        } catch (RuntimeException e) {
            throw new KiFehler("Antwort der KI ist kein gueltiges JSON");
        }
        List<KiFrage> fragen = new ArrayList<>();
        for (JsonNode f : wurzel.path("fragen")) {
            List<String> antworten = new ArrayList<>();
            f.path("antworten").forEach(x -> antworten.add(x.asString("").trim()));
            fragen.add(new KiFrage(f.path("frage").asString("").trim(), antworten,
                    f.path("correct_answer").asString("").trim(), f.path("beschreibung").asString("").trim()));
        }
        String hinweis = wurzel.path("hinweis").asString("").trim();
        return new Vorschlag(fragen, hinweis.isEmpty() ? List.of() : new ArrayList<>(List.of(hinweis)));
    }

    /** Sortiert unbrauchbare Vorschlaege aus und haelt die Regeln des Imports ein. */
    private static Vorschlag bereinige(Vorschlag v, List<QuizItem> bestehende) {
        var vorhanden = new java.util.HashSet<String>();
        bestehende.forEach(q -> vorhanden.add(vergleich(q.getQuestion())));
        List<KiFrage> gut = new ArrayList<>();
        List<String> hinweise = new ArrayList<>(v.hinweise());
        int dubletten = 0;
        int kaputt = 0;
        for (KiFrage f : v.fragen()) {
            if (f.frage().isEmpty() || f.correctAnswer().isEmpty()) {
                kaputt++;
                continue;
            }
            if (!vorhanden.add(vergleich(f.frage()))) {
                dubletten++;
                continue;
            }
            List<String> antworten = new ArrayList<>(f.antworten().stream().filter(s -> !s.isEmpty()).distinct().toList());
            if (!antworten.contains(f.correctAnswer())) {
                antworten.add(0, f.correctAnswer());
            }
            String beschreibung = f.beschreibung().length() > QuizItem.DESCRIPTION_MAX_LENGTH
                    ? f.beschreibung().substring(0, QuizItem.DESCRIPTION_MAX_LENGTH - 1) + "…"
                    : f.beschreibung();
            gut.add(new KiFrage(f.frage(), antworten, f.correctAnswer(), beschreibung));
        }
        if (dubletten > 0) {
            hinweise.add(dubletten == 1 ? "1 Vorschlag war schon vorhanden und wurde weggelassen"
                    : dubletten + " Vorschläge waren schon vorhanden und wurden weggelassen");
        }
        if (kaputt > 0) {
            hinweise.add(kaputt == 1 ? "1 unvollständiger Vorschlag weggelassen"
                    : kaputt + " unvollständige Vorschläge weggelassen");
        }
        return new Vorschlag(gut, hinweise);
    }

    private static String vergleich(String frage) {
        return frage == null ? "" : frage.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** Fehlermeldung, die der Dialog direkt anzeigen kann. */
    public static class KiFehler extends RuntimeException {
        public KiFehler(String meldung) {
            super(meldung);
        }
    }
}
