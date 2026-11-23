package de.witchcafe.quiz.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizModus;
import de.witchcafe.quiz.QuizService;

/**
 * Selbsttest zum Durchklicken: Klick auf die Frage zeigt die Antwort, Klick auf die Antwort
 * fuehrt zur naechsten Frage. Ohne Bewertung.
 */
public class SelbsttestDialog extends QuizLaufDialog {

    SelbsttestDialog(String category, QuizService quizService, QuizItemService quizItemService,
                     CurrentUser currentUser) {
        super(QuizModus.SELBSTTEST, category, quizService, quizItemService, currentUser);
        starte();
    }

    @Override
    protected void zeige(QuizItem item) {
        Component frage = frageText(item);
        frage.getElement().getStyle().set("cursor", "pointer");
        Span tipp = new Span("Antippen, um die Antwort zu sehen");
        tipp.getStyle().setFontSize("var(--lumo-font-size-s)").setColor("var(--lumo-secondary-text-color)");

        Div antwort = new Div(item.getAnswer());
        antwort.getStyle().set("cursor", "pointer")
                .setPadding("var(--lumo-space-s) var(--lumo-space-m)")
                .setBackground("var(--lumo-contrast-5pct)")
                .setBorderRadius("var(--lumo-border-radius-m)");
        antwort.setTitle("Antippen für die nächste Frage");
        antwort.setVisible(false);

        Component erklaerung = erklaerung(item);
        if (erklaerung != null) {
            erklaerung.setVisible(false);
        }
        // Bild, das die Loesung zeigt, erst zusammen mit der Antwort
        Component antwortBild = item.isBildMitAntwort() ? bild(item) : null;
        if (antwortBild != null) {
            antwortBild.setVisible(false);
        }

        frage.getElement().addEventListener("click", e -> {
            antwort.setVisible(true);
            tipp.setText("Antwort antippen für die nächste Frage");
            if (erklaerung != null) {
                erklaerung.setVisible(true);
            }
            if (antwortBild != null) {
                antwortBild.setVisible(true);
            }
        });
        antwort.addClickListener(e -> {
            beantwortet(item, true, null);
            weiter();
        });

        zeigeFrageMitBild(item, frage);
        inhalt.add(tipp, antwort);
        if (antwortBild != null) {
            inhalt.add(antwortBild);
        }
        if (erklaerung != null) {
            inhalt.add(erklaerung);
        }
    }
}
