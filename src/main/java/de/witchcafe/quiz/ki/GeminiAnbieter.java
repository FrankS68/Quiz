package de.witchcafe.quiz.ki;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import de.witchcafe.quiz.ki.KiFragenService.KiFehler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Google Gemini (generateContent); der Server liest die Links, PDFs gehen inline mit. */
@Component
class GeminiAnbieter implements KiAnbieter {

    private static final Logger log = LoggerFactory.getLogger(GeminiAnbieter.class);

    private final String apiKey;
    private final RestClient client = HttpKi.client("https://generativelanguage.googleapis.com/v1beta");
    private final JsonMapper json = JsonMapper.builder().build();

    GeminiAnbieter(@Value("${quiz.ki.gemini-api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public String name() {
        return "Gemini";
    }

    @Override
    public boolean konfiguriert() {
        return !apiKey.isEmpty();
    }

    @Override
    public String erzeuge(Anfrage a, List<String> hinweise) {
        List<DokumentLeser.Dokument> docs = DokumentLeser.lies(a.links(), hinweise);
        List<Map<String, Object>> teile = new ArrayList<>();
        teile.add(Map.of("text", a.aufgabe() + DokumentLeser.alsText(docs)));
        docs.stream().filter(DokumentLeser.Dokument::istPdf).forEach(d -> teile.add(Map.of("inline_data",
                Map.of("mime_type", "application/pdf", "data", Base64.getEncoder().encodeToString(d.pdf())))));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", a.system()))),
                "contents", List.of(Map.of("role", "user", "parts", teile)),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", ohneAdditionalProperties(a.schema())));
        try {
            String antwort = client.post().uri("/models/{modell}:generateContent", a.modell())
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(body))
                    .retrieve().body(String.class);
            JsonNode wurzel = json.readTree(antwort);
            JsonNode kandidat = wurzel.path("candidates").path(0);
            String grund = kandidat.path("finishReason").asString("");
            if ("MAX_TOKENS".equals(grund)) {
                throw new KiFehler("Antwort zu lang, bitte weniger Fragen auf einmal erzeugen");
            }
            if (kandidat.isMissingNode() || "SAFETY".equals(grund) || "PROHIBITED_CONTENT".equals(grund)) {
                throw new KiFehler("Die KI hat die Anfrage abgelehnt ("
                        + (grund.isEmpty() ? wurzel.path("promptFeedback").path("blockReason").asString("?") : grund) + ")");
            }
            StringBuilder text = new StringBuilder();
            kandidat.path("content").path("parts").forEach(p -> {
                if (!p.path("thought").asBoolean(false)) {
                    text.append(p.path("text").asString(""));
                }
            });
            log.info("KI-Fragen mit gemini {}: {}", a.modell(), wurzel.path("usageMetadata"));
            return text.toString();
        } catch (RestClientResponseException e) {
            log.warn("Gemini-Fehler: {}", e.getResponseBodyAsString());
            throw new KiFehler("Gemini-API " + e.getStatusCode().value() + ": " + HttpKi.fehlertext(e.getResponseBodyAsString()));
        }
    }

    /** Gemini kennt nur einen Teil von JSON Schema; additionalProperties lassen wir weg. */
    @SuppressWarnings("unchecked")
    private static Object ohneAdditionalProperties(Object knoten) {
        if (knoten instanceof Map<?, ?> m) {
            Map<String, Object> neu = new LinkedHashMap<>();
            ((Map<String, Object>) m).forEach((k, v) -> {
                if (!"additionalProperties".equals(k)) {
                    neu.put(k, ohneAdditionalProperties(v));
                }
            });
            return neu;
        }
        if (knoten instanceof List<?> l) {
            return l.stream().map(GeminiAnbieter::ohneAdditionalProperties).toList();
        }
        return knoten;
    }
}
