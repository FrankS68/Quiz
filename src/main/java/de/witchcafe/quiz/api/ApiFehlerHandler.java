package de.witchcafe.quiz.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Fehler der REST-API direkt als JSON beantworten. Ohne diesen Handler ginge die Antwort
 * ueber /error, und das sichert witch-auth mit Weiterleitung auf die Login-Seite ab -
 * ein API-Client saehe dann 302 statt 400/403.
 */
@RestControllerAdvice(basePackageClasses = ApiFehlerHandler.class)
public class ApiFehlerHandler {

    /** z.B. Import mit einem Key der Rolle USER (@PreAuthorize im QuizItemService). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> keineBerechtigung(AccessDeniedException e) {
        return fehler(HttpStatus.FORBIDDEN, "Keine Berechtigung: Importieren nur mit Rolle EDITOR oder ADMIN");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> ungueltigesJson(HttpMessageNotReadableException e) {
        return fehler(HttpStatus.BAD_REQUEST, "Ungueltiges JSON (UTF-8, Array von Fragen erwartet): "
                + e.getMostSpecificCause().getMessage());
    }

    private static ResponseEntity<Map<String, String>> fehler(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("fehler", text));
    }
}
