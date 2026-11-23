package de.witchcafe.quiz.ki;

import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** OpenAI (Chat Completions); PDFs gehen als Datei-Anhang mit. */
@Component
class OpenAiAnbieter extends ChatCompletionsAnbieter {

    OpenAiAnbieter(@Value("${quiz.ki.openai-api-key:}") String apiKey) {
        super("https://api.openai.com/v1", apiKey);
    }

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public String name() {
        return "OpenAI";
    }

    @Override
    Map<String, Object> pdfTeil(DokumentLeser.Dokument pdf) {
        String name = pdf.url().replaceAll("[?#].*", "").replaceAll(".*/", "");
        return Map.of("type", "file", "file", Map.of(
                "filename", name.isBlank() ? "dokument.pdf" : name,
                "file_data", "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdf.pdf())));
    }
}
