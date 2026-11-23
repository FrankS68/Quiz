package de.witchcafe.quiz.ki;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import de.witchcafe.quiz.ki.KiFragenService.KiFehler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Anbieter mit OpenAI-kompatibler Chat-Completions-API (OpenAI, Mistral). Kein Web-Fetch:
 * der Server liest die Links, Text kommt in den Prompt, PDFs als Anhang ({@link #pdfTeil}).
 */
abstract class ChatCompletionsAnbieter implements KiAnbieter {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletionsAnbieter.class);

    private final String apiKey;
    private final RestClient client;
    private final JsonMapper json = JsonMapper.builder().build();

    ChatCompletionsAnbieter(String baseUrl, String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.client = HttpKi.client(baseUrl);
    }

    @Override
    public boolean konfiguriert() {
        return !apiKey.isEmpty();
    }

    /** Inhaltsteil fuer ein PDF in der Benutzernachricht. */
    abstract Map<String, Object> pdfTeil(DokumentLeser.Dokument pdf);

    @Override
    public String erzeuge(Anfrage a, List<String> hinweise) {
        List<DokumentLeser.Dokument> docs = DokumentLeser.lies(a.links(), hinweise);
        List<Map<String, Object>> inhalt = new ArrayList<>();
        inhalt.add(Map.of("type", "text", "text", a.aufgabe() + DokumentLeser.alsText(docs)));
        docs.stream().filter(DokumentLeser.Dokument::istPdf).forEach(d -> inhalt.add(pdfTeil(d)));

        Map<String, Object> body = Map.of(
                "model", a.modell(),
                "messages", List.of(
                        Map.of("role", "system", "content", a.system()),
                        Map.of("role", "user", "content", inhalt)),
                "response_format", Map.of("type", "json_schema",
                        "json_schema", Map.of("name", "quizfragen", "strict", true, "schema", a.schema())));
        try {
            String antwort = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(body))
                    .retrieve().body(String.class);
            JsonNode wurzel = json.readTree(antwort);
            JsonNode wahl = wurzel.path("choices").path(0);
            if ("length".equals(wahl.path("finish_reason").asString(""))) {
                throw new KiFehler("Antwort zu lang, bitte weniger Fragen auf einmal erzeugen");
            }
            String verweigert = wahl.path("message").path("refusal").asString("");
            if (!verweigert.isBlank()) {
                throw new KiFehler("Die KI hat die Anfrage abgelehnt: " + verweigert);
            }
            log.info("KI-Fragen mit {} {}: {}", id(), a.modell(), wurzel.path("usage"));
            JsonNode antwortInhalt = wahl.path("message").path("content");
            if (!antwortInhalt.isArray()) {
                return antwortInhalt.asString("");
            }
            // Denkende Modelle liefern Bloecke: {"type":"thinking",...}, {"type":"text","text":"..."}
            StringBuilder text = new StringBuilder();
            antwortInhalt.forEach(b -> {
                if ("text".equals(b.path("type").asString(""))) {
                    text.append(b.path("text").asString(""));
                }
            });
            return text.toString();
        } catch (RestClientResponseException e) {
            log.warn("{}-Fehler: {}", name(), e.getResponseBodyAsString());
            throw new KiFehler(name() + "-API " + e.getStatusCode().value() + ": " + HttpKi.fehlertext(e.getResponseBodyAsString()));
        }
    }
}
