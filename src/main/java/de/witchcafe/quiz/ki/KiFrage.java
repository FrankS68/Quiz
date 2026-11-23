package de.witchcafe.quiz.ki;

import java.util.List;

/**
 * Eine von der KI vorgeschlagene Frage (noch nicht gespeichert). Die Kategorie (thema)
 * setzt der Dialog, nicht die KI.
 */
public record KiFrage(String frage, List<String> antworten, String correctAnswer, String beschreibung) {
}
