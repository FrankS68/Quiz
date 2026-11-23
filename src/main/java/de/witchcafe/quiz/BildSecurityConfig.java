package de.witchcafe.quiz;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Fragen-Bilder ({@link BildController}): nur lesen, nur fuer angemeldete Benutzer. Die Anmeldung
 * kommt aus der Sitzung von witch-auth; die witch-auth/Vaadin-Kette selbst wuerde den Pfad mit 403
 * ablehnen, deshalb eine eigene Kette mit securityMatcher (wie bei der REST-API).
 */
@Configuration
public class BildSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain bildFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(BildController.PFAD + "**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, BildController.PFAD + "**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .requestCache(c -> c.disable());
        return http.build();
    }
}
