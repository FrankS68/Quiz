package de.witchcafe.quiz.ki;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.beta.messages.BetaJsonOutputFormat;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaWebFetchTool20260209;
import com.anthropic.models.beta.messages.MessageCreateParams;

import de.witchcafe.quiz.ki.KiFragenService.KiFehler;

/**
 * Claude ueber die Anthropic-API. Liest die Dokument-Links selbst mit dem Web-Fetch-Tool
 * (auch PDFs) und liefert strukturiertes JSON.
 */
@Component
class ClaudeAnbieter implements KiAnbieter {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnbieter.class);

    private final String apiKey;
    private volatile AnthropicClient client;

    ClaudeAnbieter(@Value("${quiz.ki.anthropic-api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String id() {
        return "anthropic";
    }

    @Override
    public String name() {
        return "Claude";
    }

    @Override
    public boolean konfiguriert() {
        return !apiKey.isEmpty();
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                }
            }
        }
        return client;
    }

    @Override
    public String erzeuge(Anfrage a, List<String> hinweise) {
        BetaJsonOutputFormat.Schema.Builder schema = BetaJsonOutputFormat.Schema.builder();
        a.schema().forEach((k, v) -> schema.putAdditionalProperty(k, JsonValue.from(v)));

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(a.modell())
                .maxTokens(32000L)
                .system(a.system())
                .outputConfig(BetaOutputConfig.builder()
                        .format(BetaJsonOutputFormat.builder().schema(schema.build()).build())
                        .build())
                // Lehnen die Sicherheitsfilter ab, antwortet serverseitig ein passendes Ersatzmodell
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .addUserMessage(a.aufgabe());
        if (!a.links().isEmpty()) {
            // Web-Fetch holt nur URLs, die im Gespraech stehen, also die aus dem Auftrag
            params.addTool(BetaWebFetchTool20260209.builder().maxUses((long) Math.min(20, a.links().size() * 3)).build());
        }
        try {
            BetaMessage antwort = null;
            for (int runde = 0; runde < 5; runde++) {
                antwort = client().beta().messages().create(params.build());
                if (antwort.stopReason().orElse(null) != BetaStopReason.PAUSE_TURN) {
                    break;
                }
                // Rundenlimit der Server-Tools erreicht: mit der bisherigen Antwort fortsetzen
                params.addMessage(antwort);
            }
            BetaStopReason grund = antwort.stopReason().orElse(null);
            if (grund == BetaStopReason.REFUSAL) {
                throw new KiFehler("Die KI hat die Anfrage abgelehnt");
            }
            if (grund == BetaStopReason.MAX_TOKENS) {
                throw new KiFehler("Antwort zu lang, bitte weniger Fragen auf einmal erzeugen");
            }
            log.info("KI-Fragen mit anthropic {}: {}", a.modell(), antwort.usage());
            // die strukturierte Ausgabe steht im letzten Textblock (davor ggf. Web-Fetch-Bloecke)
            return antwort.content().stream().flatMap(b -> b.text().stream()).map(t -> t.text())
                    .reduce("", (x, y) -> y);
        } catch (AnthropicServiceException e) {
            log.warn("Anthropic-Fehler: {}", e.getMessage());
            String roh = String.valueOf(e.getMessage());
            String text = HttpKi.fehlertext(roh.contains("{") ? roh.substring(roh.indexOf('{')) : roh);
            throw new KiFehler("Anthropic-API " + e.statusCode() + ": " + text);
        }
    }
}
