package de.witchcafe.quiz.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.quiz.Lueckentext;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizModus;
import de.witchcafe.quiz.QuizService;

/**
 * Lueckentext: die Antwort wird mit Luecken fuer die signifikanten Stichworte gezeigt
 * (siehe {@link Lueckentext}), die eingetippt werden muessen. Eine Frage zaehlt als richtig,
 * wenn alle Luecken stimmen; kleine Tippfehler werden toleriert.
 */
public class LueckentextDialog extends QuizLaufDialog {

    LueckentextDialog(String category, QuizService quizService, QuizItemService quizItemService,
                      CurrentUser currentUser) {
        super(QuizModus.LUECKENTEXT, category, quizService, quizItemService, currentUser);
        starte();
    }

    /** Eine Luecke mit ihrem Eingabefeld. */
    private record Luecke(String loesung, TextField feld) {
    }

    @Override
    protected void zeige(QuizItem item) {
        Lueckentext lt = Lueckentext.aus(item.getQuestion(), item.getAnswer()).orElseThrow();

        Div text = new Div();
        text.getStyle().set("line-height", "2.6");
        List<Luecke> luecken = new ArrayList<>();
        for (Lueckentext.Teil teil : lt.teile()) {
            if (!teil.luecke()) {
                text.add(new Span(teil.text()));
                continue;
            }
            TextField feld = new TextField();
            feld.addThemeVariants(TextFieldVariant.LUMO_SMALL);
            feld.setWidth(Math.min(16, Math.max(5, teil.text().length() + 2)) + "ch");
            feld.getStyle().set("vertical-align", "middle").setMargin("0 0.2em");
            feld.setAriaLabel("Lücke " + (luecken.size() + 1));
            boolean ziffern = teil.text().chars().anyMatch(Character::isDigit);
            if (!ziffern && teil.text().length() >= 6) {
                feld.setPlaceholder(teil.text().charAt(0) + "…"); // Anfangsbuchstabe als Tipp
            }
            luecken.add(new Luecke(teil.text(), feld));
            text.add(feld);
        }

        Button pruefen = new Button("Prüfen");
        pruefen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        boolean[] geprueft = {false};
        Runnable pruefe = () -> {
            if (geprueft[0]) {
                return;
            }
            geprueft[0] = true;
            pruefen.setVisible(false);
            aufloesen(item, text, luecken);
        };
        pruefen.addClickListener(e -> pruefe.run());
        // Enter springt zur naechsten Luecke (direkt im Browser, damit schnell weitergetippte
        // Zeichen nicht im alten Feld landen); in der letzten Luecke wird geprueft
        for (int i = 0; i + 1 < luecken.size(); i++) {
            luecken.get(i).feld().getElement().executeJs(
                    "const n = $0; this.addEventListener('keydown', e => {"
                            + " if (e.key === 'Enter') { e.preventDefault(); n.focus(); } });",
                    luecken.get(i + 1).feld().getElement());
        }
        luecken.get(luecken.size() - 1).feld().addKeyDownListener(Key.ENTER, e -> pruefe.run());

        Span hinweis = new Span(luecken.size() == 1 ? "Ergänze das fehlende Stichwort:"
                : "Ergänze die " + luecken.size() + " fehlenden Stichworte:");
        hinweis.getStyle().setFontSize("var(--lumo-font-size-s)").setColor("var(--lumo-secondary-text-color)");
        inhalt.add(frageText(item), hinweis, text, pruefen);
        luecken.get(0).feld().focus();
    }

    private void aufloesen(QuizItem item, Div text, List<Luecke> luecken) {
        boolean alleRichtig = true;
        List<String> eingaben = new ArrayList<>();
        for (Luecke l : luecken) {
            String eingabe = l.feld().getValue().trim();
            eingaben.add(eingabe.isEmpty() ? "…" : eingabe);
            boolean ok = Lueckentext.passt(eingabe, l.loesung());
            alleRichtig &= ok;
            int pos = text.getElement().indexOfChild(l.feld().getElement());
            text.getElement().setChild(pos, ergebnis(eingabe, l.loesung(), ok).getElement());
        }
        beantwortet(item, alleRichtig, alleRichtig ? null : String.join(" · ", eingaben));
        Component erklaerung = erklaerung(item);
        if (erklaerung != null) {
            inhalt.add(erklaerung);
        }
        Button weiter = weiterButton();
        inhalt.add(weiter);
        weiter.focus();
    }

    /** Aufgeloeste Luecke: richtig gruen, falsch die Eingabe durchgestrichen und die Loesung gruen. */
    private static Component ergebnis(String eingabe, String loesung, boolean ok) {
        Span richtig = new Span(loesung);
        richtig.getStyle().setFontWeight("600").setColor("var(--lumo-success-text-color)")
                .setBackground("var(--lumo-success-color-10pct)")
                .setBorderRadius("var(--lumo-border-radius-s)").setPadding("0 0.25em");
        if (ok) {
            richtig.setTitle(eingabe.equals(loesung) ? "Richtig" : "Richtig (deine Eingabe: " + eingabe + ")");
            return richtig;
        }
        Span falsch = new Span(eingabe.isEmpty() ? "…" : eingabe);
        falsch.getStyle().setColor("var(--lumo-error-text-color)").set("text-decoration", "line-through")
                .setMargin("0 0.25em 0 0");
        return new Span(falsch, richtig);
    }
}
