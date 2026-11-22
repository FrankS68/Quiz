package de.witchcafe.quiz.api;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Prueft den Header {@code Authorization: Bearer <token>} fuer /api/** und setzt bei
 * gueltigem Token die Rolle des Keys in den SecurityContext (analog knownet).
 * Kein @Component, damit Spring Boot ihn nicht zusaetzlich als Servlet-Filter fuer alle
 * Pfade registriert; er haengt nur in der API-Filterkette (QuizApiSecurityConfig).
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            apiKeyService.findByToken(header.substring(7).trim()).ifPresent(apiKey -> {
                var auth = new UsernamePasswordAuthenticationToken(apiKey.getName(), null, List.of(
                        new SimpleGrantedAuthority("ROLE_" + apiKey.getRolle()),
                        new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        filterChain.doFilter(request, response);
    }
}
