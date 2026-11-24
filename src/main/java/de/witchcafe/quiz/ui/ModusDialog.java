package de.witchcafe.quiz.ui;

import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.witchcafe.quiz.QuizModus;
import de.witchcafe.ui.Responsive;

/** Auswahl der Art des Quiz-Laufs vor dem Start. */
public class ModusDialog extends Dialog {

    public ModusDialog(String category, Consumer<QuizModus> gewaehlt) {
        setHeaderTitle("Quiz starten: " + category.replace("/", " › "));
        VerticalLayout liste = new VerticalLayout();
        liste.setPadding(false);
        for (QuizModus modus : QuizModus.values()) {
            liste.add(knopf(modus, gewaehlt));
        }
        add(liste);
        getFooter().add(new Button("Abbrechen", e -> close()));
        Responsive.dialog(this, "26em");
    }

    private Button knopf(QuizModus modus, Consumer<QuizModus> gewaehlt) {
        Span titel = new Span(modus.titel());
        titel.getStyle().setFontWeight("600");
        Span text = new Span(modus.beschreibung() + " · " + modus.fragenProLauf() + " Fragen");
        text.getStyle().setFontSize("var(--lumo-font-size-s)").setColor("var(--lumo-secondary-text-color)")
                .set("white-space", "normal");
        VerticalLayout beschriftung = new VerticalLayout(titel, text);
        beschriftung.setPadding(false);
        beschriftung.setSpacing(false);

        var icon = switch (modus) {
            case SELBSTTEST -> VaadinIcon.EYE.create();
            case MULTIPLE_CHOICE -> VaadinIcon.LIST_OL.create();
            case LUECKENTEXT -> VaadinIcon.KEYBOARD.create();
        };
        HorizontalLayout zeile = new HorizontalLayout(icon, beschriftung);
        zeile.setAlignItems(FlexComponent.Alignment.CENTER);

        Button b = new Button(zeile, e -> {
            close();
            gewaehlt.accept(modus);
        });
        b.setWidthFull();
        b.getStyle().set("height", "auto").set("text-align", "left")
                .setPadding("var(--lumo-space-s) var(--lumo-space-m)");
        return b;
    }
}
