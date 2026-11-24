package de.witchcafe.quiz;

/**
 * Art eines Quiz-Laufs. Gespeichert als Name in {@code quiz.modus}; alte Eintraege ohne
 * Modus (vor 2026-11-23) gelten als {@link #SELBSTTEST}.
 */
public enum QuizModus {

    /** Frage anklicken zeigt die Antwort, Klick auf die Antwort fuehrt weiter - ohne Bewertung. */
    SELBSTTEST("Selbsttest", "Frage ansehen, Antwort aufdecken, selbst einschätzen", 5, false),

    /** Auswahl aus den Antwortoptionen der Frage, mit Auswertung am Ende. */
    MULTIPLE_CHOICE("Multiple Choice", "Richtige Antwort aus mehreren Vorschlägen wählen, mit Ergebnis", 10, true),

    /** Die Antwort als Lueckentext: die wichtigsten Stichworte muessen eingetippt werden. */
    LUECKENTEXT("Lückentext", "Wichtige Stichworte der Antwort selbst eintippen, mit Ergebnis", 5, true);

    private final String titel;
    private final String beschreibung;
    private final int fragenProLauf;
    private final boolean bewertet;

    QuizModus(String titel, String beschreibung, int fragenProLauf, boolean bewertet) {
        this.titel = titel;
        this.beschreibung = beschreibung;
        this.fragenProLauf = fragenProLauf;
        this.bewertet = bewertet;
    }

    public String titel() {
        return titel;
    }

    public String beschreibung() {
        return beschreibung;
    }

    public int fragenProLauf() {
        return fragenProLauf;
    }

    /** true: der Lauf zaehlt richtige Antworten und zeigt am Ende ein Ergebnis. */
    public boolean bewertet() {
        return bewertet;
    }

    /** Ob sich eine Frage fuer diesen Modus eignet. */
    public boolean eignetSich(QuizItem item) {
        return switch (this) {
            case SELBSTTEST -> true;
            case MULTIPLE_CHOICE -> MultipleChoice.optionen(item).size() >= 2;
            case LUECKENTEXT -> Lueckentext.aus(item.getQuestion(), item.getAnswer()).isPresent();
        };
    }
}
