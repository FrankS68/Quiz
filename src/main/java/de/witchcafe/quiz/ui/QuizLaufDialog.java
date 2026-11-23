package de.witchcafe.quiz.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizModus;
import de.witchcafe.quiz.QuizService;
import de.witchcafe.ui.Responsive;

/**
 * Gemeinsamer Rahmen aller Quiz-Laeufe: waehlt zufaellig bis zu {@link QuizModus#fragenProLauf()}
 * passende Fragen der Kategorie inkl. Unterbereiche, zeigt sie nacheinander und speichert den Lauf
 * am Ende (oder mit "Beenden"); {@code result} enthaelt die IDs der gespielten Fragen als JSON-Array.
 * Bewertete Modi zeigen danach das Ergebnis mit den falsch beantworteten Fragen.
 * Schliessen ohne Beenden speichert nichts.
 */
public abstract class QuizLaufDialog extends Dialog {

    /** Neuen Lauf im gewuenschten Modus erzeugen. */
    public static QuizLaufDialog fuer(QuizModus modus, String category, QuizService quizService,
                                      QuizItemService quizItemService, CurrentUser currentUser) {
        return switch (modus) {
            case SELBSTTEST -> new SelbsttestDialog(category, quizService, quizItemService, currentUser);
            case MULTIPLE_CHOICE -> new MultipleChoiceDialog(category, quizService, quizItemService, currentUser);
            case LUECKENTEXT -> new LueckentextDialog(category, quizService, quizItemService, currentUser);
        };
    }

    /** Eine falsch beantwortete Frage fuer die Auswertung. */
    private record Fehler(QuizItem item, @Nullable String eingabe) {
    }

    private final QuizModus modus;
    private final String category;
    private final QuizService quizService;
    private final CurrentUser currentUser;

    private final List<QuizItem> fragen;
    private final List<Long> gespielt = new ArrayList<>();
    private final List<Fehler> fehler = new ArrayList<>();
    private int index = 0;
    private int richtig = 0;
    private boolean gespeichert = false;

    private final Span fortschritt = new Span();
    private final Button beendenButton;
    private final Button schliessenButton;
    /** Inhalt der aktuellen Frage; die Unterklassen fuellen ihn in {@link #zeige(QuizItem)}. */
    protected final VerticalLayout inhalt = new VerticalLayout();

    protected QuizLaufDialog(QuizModus modus, String category, QuizService quizService,
                             QuizItemService quizItemService, CurrentUser currentUser) {
        this.modus = modus;
        this.category = category;
        this.quizService = quizService;
        this.currentUser = currentUser;

        List<QuizItem> passend = new ArrayList<>(quizItemService.findImTeilbaum(category).stream()
                .filter(modus::eignetSich).toList());
        Collections.shuffle(passend);
        fragen = passend.size() > modus.fragenProLauf() ? passend.subList(0, modus.fragenProLauf()) : passend;

        setHeaderTitle(modus.titel() + ": " + category.replace("/", " › "));
        fortschritt.getStyle().setColor("var(--lumo-secondary-text-color)")
                .setFontSize("var(--lumo-font-size-s)").setWhiteSpace(com.vaadin.flow.dom.Style.WhiteSpace.NOWRAP);
        getHeader().add(fortschritt);

        beendenButton = new Button("Beenden", e -> beenden());
        beendenButton.setTooltipText("Lauf jetzt beenden und speichern");
        schliessenButton = new Button("Schließen", e -> close());
        schliessenButton.setTooltipText("Ohne Speichern schließen");
        getFooter().add(beendenButton, schliessenButton);

        inhalt.setPadding(false);
        inhalt.setSpacing(true);
        add(inhalt);
        Responsive.dialog(this, "34em");
    }

    /** Erste Frage anzeigen; von den Unterklassen am Ende ihres Konstruktors aufzurufen. */
    protected final void starte() {
        if (fragen.isEmpty()) {
            inhalt.add(new Span(modus == QuizModus.SELBSTTEST
                    ? "In dieser Kategorie gibt es noch keine Fragen."
                    : "In dieser Kategorie gibt es keine Fragen, die sich für " + modus.titel() + " eignen."));
            beendenButton.setVisible(false);
            return;
        }
        naechste();
    }

    /** Zeigt die Frage im {@link #inhalt} (vorher geleert). */
    protected abstract void zeige(QuizItem item);

    /** Die aktuelle Frage ist beantwortet; {@code eingabe} erscheint bei falschen Antworten in der Auswertung. */
    protected final void beantwortet(QuizItem item, boolean korrekt, @Nullable String eingabe) {
        gespielt.add(item.getId());
        if (korrekt) {
            richtig++;
        } else {
            fehler.add(new Fehler(item, eingabe));
        }
    }

    /** Zur naechsten Frage; nach der letzten wird der Lauf beendet. */
    protected final void weiter() {
        index++;
        if (index >= fragen.size()) {
            beenden();
            return;
        }
        naechste();
    }

    private void naechste() {
        fortschritt.setText("Frage " + (index + 1) + " von " + fragen.size());
        inhalt.removeAll();
        zeige(fragen.get(index));
    }

    /** Knopf "Weiter"/"Zum Ergebnis" fuer die Unterklassen, nach dem Aufloesen einer Frage. */
    protected final Button weiterButton() {
        boolean letzte = index + 1 >= fragen.size();
        Button b = new Button(letzte ? (modus.bewertet() ? "Zum Ergebnis" : "Fertig") : "Weiter", e -> weiter());
        b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        b.addClickShortcut(Key.ENTER);
        return b;
    }

    /** Frage-Text, gross und fett. */
    protected static Component frageText(QuizItem item) {
        Div div = new Div(item.getQuestion());
        div.getStyle().setFontWeight("600").setFontSize("var(--lumo-font-size-l)");
        return div;
    }

    /** Bild der Frage (falls vorhanden), passend in den Dialog skaliert; sonst {@code null}. */
    protected static @Nullable Component bild(QuizItem item) {
        return item.hatBild() ? bildKomponente(item.getBild()) : null;
    }

    /** Bild zu einer URL, hoechstens so breit wie der Dialog und 16em hoch. */
    static Component bildKomponente(String url) {
        Image img = new Image(url, "Bild zur Frage");
        img.getStyle().setMaxWidth("100%").setMaxHeight("16em").set("object-fit", "contain")
                .setDisplay(com.vaadin.flow.dom.Style.Display.BLOCK)
                .setMargin("0 auto").setBorderRadius("var(--lumo-border-radius-m)");
        img.getElement().setAttribute("loading", "eager");
        img.getElement().setAttribute("referrerpolicy", "no-referrer");
        return img;
    }

    /** Fragetext und darunter ggf. das Bild in den {@link #inhalt}. */
    protected final void zeigeFrageMitBild(QuizItem item, Component frage) {
        inhalt.add(frage);
        Component b = bild(item);
        if (b != null) {
            inhalt.add(b);
        }
    }

    /** Beschreibung/Erklaerung der Frage nach dem Aufloesen, falls vorhanden. */
    protected static @Nullable Component erklaerung(QuizItem item) {
        String d = item.getDescription();
        if (d == null || d.isBlank()) {
            return null;
        }
        Div div = new Div(d);
        div.getStyle().setFontSize("var(--lumo-font-size-s)").setColor("var(--lumo-secondary-text-color)");
        return div;
    }

    private void beenden() {
        if (gespeichert) {
            return;
        }
        gespeichert = true;
        if (gespielt.isEmpty()) {
            close();
            return;
        }
        quizService.speichereLauf(category, currentUser.get().orElse(null), modus,
                new Gson().toJson(gespielt), modus.bewertet() ? richtig : null);
        Notification.show("Quiz gespeichert", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        if (modus.bewertet()) {
            zeigeErgebnis();
        } else {
            close();
        }
    }

    private void zeigeErgebnis() {
        int anzahl = gespielt.size();
        int prozent = Math.round(100f * richtig / anzahl);
        fortschritt.setText("Ergebnis");
        beendenButton.setVisible(false);
        schliessenButton.setText("Schließen");
        schliessenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        schliessenButton.setTooltipText(null);
        inhalt.removeAll();

        H3 titel = new H3(richtig + " von " + anzahl + " richtig (" + prozent + " %)");
        titel.getStyle().setMargin("0");
        ProgressBar balken = new ProgressBar(0, anzahl, richtig);
        balken.addThemeVariants(prozent >= 75 ? ProgressBarVariant.LUMO_SUCCESS
                : prozent >= 50 ? ProgressBarVariant.LUMO_CONTRAST
                : ProgressBarVariant.LUMO_ERROR);
        inhalt.add(titel, balken);

        if (fehler.isEmpty()) {
            inhalt.add(new Span("Alles richtig – sehr gut!"));
            return;
        }
        Span hinweis = new Span("Diese Fragen solltest du dir noch einmal ansehen:");
        hinweis.getStyle().setFontWeight("600");
        inhalt.add(hinweis);
        for (Fehler f : fehler) {
            Div frage = new Div(f.item().getQuestion());
            frage.getStyle().setFontWeight("600");
            Div loesung = new Div("✓ " + f.item().getAnswer());
            loesung.getStyle().setColor("var(--lumo-success-text-color)");
            VerticalLayout block = new VerticalLayout(frage);
            if (f.eingabe() != null && !f.eingabe().isBlank()) {
                Div eingabe = new Div("✗ " + f.eingabe());
                eingabe.getStyle().setColor("var(--lumo-error-text-color)");
                block.add(eingabe);
            }
            block.add(loesung);
            block.setPadding(false);
            block.setSpacing(false);
            block.getStyle().setBorderLeft("3px solid var(--lumo-error-color)").setPaddingLeft("var(--lumo-space-s)");
            inhalt.add(block);
        }
    }
}
