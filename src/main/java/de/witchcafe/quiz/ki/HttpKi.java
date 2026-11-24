package de.witchcafe.quiz.ki;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Gemeinsamer HTTP-Client fuer die REST-Anbieter (lange Antwortzeiten). */
final class HttpKi {

    private HttpKi() {
    }

    /** Kurzer Fehlertext aus einer JSON-Fehlerantwort (error.message, message oder detail). */
    static String fehlertext(String body) {
        try {
            var n = tools.jackson.databind.json.JsonMapper.builder().build().readTree(body);
            for (var kandidat : java.util.List.of(n.path("error").path("message"), n.path("message"), n.path("detail"))) {
                if (kandidat.isString() && !kandidat.asString().isBlank()) {
                    return kandidat.asString();
                }
            }
        } catch (RuntimeException e) {
            // kein JSON
        }
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }

    static RestClient client(String baseUrl) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMinutes(10));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
