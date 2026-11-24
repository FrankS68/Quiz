package de.witchcafe.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Antwortoptionen einer Frage fuer den Modus Multiple Choice. */
public final class MultipleChoice {

    private MultipleChoice() {
    }

    /**
     * Die Optionen der Frage ohne Leer- und Doppeleintraege; die richtige Antwort ist immer dabei
     * (auch wenn sie in {@code options} fehlt). Weniger als 2 Optionen: fuer Multiple Choice ungeeignet.
     */
    public static List<String> optionen(QuizItem item) {
        Set<String> optionen = new LinkedHashSet<>();
        if (item.getOptions() != null) {
            for (String o : item.getOptions()) {
                if (o != null && !o.isBlank()) {
                    optionen.add(o.trim());
                }
            }
        }
        if (optionen.size() < 2) {
            return List.of();
        }
        optionen.add(item.getAnswer().trim());
        return List.copyOf(optionen);
    }

    /** Die Optionen in zufaelliger Reihenfolge. */
    public static List<String> gemischt(QuizItem item) {
        List<String> liste = new ArrayList<>(optionen(item));
        Collections.shuffle(liste);
        return liste;
    }

    public static boolean istRichtig(QuizItem item, String gewaehlt) {
        return item.getAnswer().trim().equals(gewaehlt);
    }
}
