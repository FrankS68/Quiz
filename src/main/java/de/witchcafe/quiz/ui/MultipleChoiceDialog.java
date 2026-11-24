package de.witchcafe.quiz.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.quiz.MultipleChoice;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizModus;
import de.witchcafe.quiz.QuizService;

/**
 * Multiple Choice: die Antwortoptionen der Frage in zufaelliger Reihenfolge als Knoepfe.
 * Nach dem Antippen wird die richtige Antwort gruen, eine falsche Wahl rot markiert.
 */
public class MultipleChoiceDialog extends QuizLaufDialog {

    MultipleChoiceDialog(String category, QuizService quizService, QuizItemService quizItemService,
                         CurrentUser currentUser) {
        super(QuizModus.MULTIPLE_CHOICE, category, quizService, quizItemService, currentUser);
        starte();
    }

    /** Eine Antwort-Kachel; eigene Div statt Button, weil vaadin-button lange Texte abschneidet. */
    private record Option(String text, Div kachel) {
    }

    @Override
    protected void zeige(QuizItem item) {
        VerticalLayout optionenBox = new VerticalLayout();
        optionenBox.setPadding(false);
        optionenBox.setSpacing(true);
        List<Option> optionen = new ArrayList<>();
        boolean[] aufgeloest = {false}; // nur die erste Wahl zaehlt
        for (String text : MultipleChoice.gemischt(item)) {
            Div kachel = new Div(text);
            kachel.setWidthFull();
            kachel.getElement().setAttribute("role", "button").setAttribute("tabindex", "0");
            kachel.getStyle().set("cursor", "pointer").set("box-sizing", "border-box")
                    .setPadding("var(--lumo-space-s) var(--lumo-space-m)")
                    .setBorderRadius("var(--lumo-border-radius-m)")
                    .setBackground("var(--lumo-contrast-5pct)")
                    .setColor("var(--lumo-primary-text-color)").setFontWeight("500");
            Runnable waehle = () -> {
                if (!aufgeloest[0]) {
                    aufgeloest[0] = true;
                    aufloesen(item, text, optionen);
                }
            };
            kachel.addClickListener(e -> waehle.run());
            kachel.getElement().addEventListener("keydown", e -> waehle.run())
                    .setFilter("event.key === 'Enter' || event.key === ' '");
            optionen.add(new Option(text, kachel));
            optionenBox.add(kachel);
        }
        zeigeFrageMitBild(item, frageText(item));
        inhalt.add(optionenBox);
    }

    private void aufloesen(QuizItem item, String gewaehlt, List<Option> optionen) {
        boolean korrekt = MultipleChoice.istRichtig(item, gewaehlt);
        for (Option o : optionen) {
            Div k = o.kachel();
            k.getStyle().set("cursor", "default");
            k.getElement().removeAttribute("tabindex");
            if (MultipleChoice.istRichtig(item, o.text())) {
                k.setText("✓ " + o.text());
                k.getStyle().setBackground("var(--lumo-success-color)").setColor("var(--lumo-success-contrast-color)");
            } else if (o.text().equals(gewaehlt)) {
                k.setText("✗ " + o.text());
                k.getStyle().setBackground("var(--lumo-error-color)").setColor("var(--lumo-error-contrast-color)");
            } else {
                k.getStyle().setColor("var(--lumo-disabled-text-color)");
            }
        }
        beantwortet(item, korrekt, korrekt ? null : gewaehlt);
        zeigeBildZurAntwort(item);
        Component erklaerung = erklaerung(item);
        if (erklaerung != null) {
            inhalt.add(erklaerung);
        }
        Button weiter = weiterButton();
        inhalt.add(weiter);
        weiter.focus();
    }
}
