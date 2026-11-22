package de.witchcafe.quiz.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security fuer die REST-API (analog knownet). Login und Vaadin-Oberflaeche sichert witch-auth;
 * hier stehen nur eigene Filterketten mit securityMatcher, damit sie die witch-auth-Kette
 * nicht ueberschreiben.
 */
@Configuration
public class QuizApiSecurityConfig {

    /** API-Doku: oeffentlich lesbar (Agenten holen sich dort Version und Beschreibung). */
    @Bean
    @Order(0)
    SecurityFilterChain apiDocsFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/docs", "/api/docs/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /** Uebrige API: nur mit "Authorization: Bearer &lt;API-Key&gt;", ohne Sitzung. */
    @Bean
    @Order(2)
    SecurityFilterChain apiKeyFilterChain(HttpSecurity http, ApiKeyService apiKeyService) throws Exception {
        http.securityMatcher("/api/**")
                .addFilterBefore(new ApiKeyFilter(apiKeyService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Ohne gueltigen Key 401 statt Weiterleitung auf die Login-Seite
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.setStatus(HttpStatus.FORBIDDEN.value())))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
