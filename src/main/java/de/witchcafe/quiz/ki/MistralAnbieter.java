package de.witchcafe.quiz.ki;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mistral (Chat Completions); PDFs liest Mistral selbst ueber die URL. */
@Component
class MistralAnbieter extends ChatCompletionsAnbieter {

    MistralAnbieter(@Value("${quiz.ki.mistral-api-key:}") String apiKey) {
        super("https://api.mistral.ai/v1", apiKey);
    }

    @Override
    public String id() {
        return "mistral";
    }

    @Override
    public String name() {
        return "Mistral";
    }

    @Override
    Map<String, Object> pdfTeil(DokumentLeser.Dokument pdf) {
        return Map.of("type", "document_url", "document_url", pdf.url());
    }
}
