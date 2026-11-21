package de.witchcafe.quiz.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Kopf der OpenAPI-Beschreibung unter /api/docs/openapi.json (Endpunkte erzeugt springdoc). */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI quizOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quiz API")
                        .version(ApiDokuController.VERSION)
                        .description("Fragen und Kategorien des Quiz lesen und Fragenkataloge importieren."))
                .components(new Components().addSecuritySchemes("apiKey",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .description("API-Key aus der Verwaltung (Admin → API-Keys)")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
