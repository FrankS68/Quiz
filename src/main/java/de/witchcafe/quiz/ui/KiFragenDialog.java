package de.witchcafe.quiz.ui;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.LitRenderer;

import de.witchcafe.quiz.Kategorie;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.ki.KiFrage;
import de.witchcafe.quiz.ki.KiFragenService;
import de.witchcafe.quiz.ki.KiFragenService.Auftrag;
import de.witchcafe.quiz.ki.KiFragenService.KiFehler;
import de.witchcafe.quiz.ki.KiFragenService.Vorschlag;

/**
 * Kategorie anlegen oder erweitern: Idee, optionale Dokument-Links und Anzahl an eine KI geben,
 * die Vorschlaege pruefen und die ausgewaehlten Fragen importieren.
 */
class KiFragenDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(KiFragenDialog.class);

    private final KiFragenService service;
    private final Runnable nachImport;

    private final ComboBox<String> kategorie = new ComboBox<>("Kategorie");
    private final TextArea idee = new TextArea("Idee bzw. Erweiterung");
    private final TextArea links = new TextArea("Links auf Dokumente (optional)");
    private final IntegerField anzahl = new IntegerField("Anzahl Fragen");
    private final Select<String> modell = new Select<>();
    private final Button erzeugen = new Button("Fragen erzeugen", VaadinIcon.MAGIC.create());
    private final VerticalLayout formular = new VerticalLayout();
    private final Paragraph fehlerText = new Paragraph();

    KiFragenDialog(KiFragenService service, List<String> kategorien, String vorbelegt, Runnable nachImport) {
        this.service = service;
        this.nachImport = nachImport;

        setHeaderTitle("Fragen mit KI erzeugen");
        setWidth("min(52em, 96vw)");
        setCloseOnOutsideClick(false);

        kategorie.setItems(kategorien);
        kategorie.setAllowCustomValue(true);
        kategorie.addCustomValueSetListener(e -> kategorie.setValue(e.getDetail()));
        kategorie.setHelperText("Bestehende wählen oder neuen Pfad eintippen, Ebenen mit / trennen");
        kategorie.setRequired(true);
        kategorie.setWidthFull();
        if (vorbelegt != null && !vorbelegt.isBlank()) {
            kategorie.setValue(vorbelegt);
        }

        idee.setPlaceholder("Worum soll es gehen? Was soll ergänzt werden, welcher Schwerpunkt, welches Niveau?");
        idee.setMinRows(3);
        idee.setWidthFull();

        links.setPlaceholder("https://… (eine Adresse pro Zeile)");
        links.setHelperText("Der Stoff dieser Dokumente (Webseiten, PDFs) wird abgefragt. Nur öffentlich erreichbare Adressen.");
        links.setMinRows(2);
        links.setWidthFull();

        anzahl.setMin(1);
        anzahl.setMax(KiFragenService.MAX_FRAGEN);
        anzahl.setValue(10);
        anzahl.setStepButtonsVisible(true);

        modell.setLabel("Modell");
        modell.setWidth("18em");
        modell.setItems(service.verfuegbareModelle());
        modell.setItemLabelGenerator(service::anzeige);
        modell.setValue(service.verfuegbareModelle().isEmpty() ? null : service.verfuegbareModelle().get(0));

        Div zeile = new Div(anzahl, modell);
        zeile.getStyle().setDisplay(com.vaadin.flow.dom.Style.Display.FLEX).set("gap", "var(--lumo-space-m)")
                .set("flex-wrap", "wrap");

        formular.setPadding(false);
        formular.add(kategorie, idee, links, zeile);
        fehlerText.getStyle().setColor("var(--lumo-error-text-color)").setMargin("0");
        fehlerText.setVisible(false);
        add(formular, fehlerText);

        erzeugen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        erzeugen.addClickListener(e -> starte());
        Button abbrechen = new Button("Abbrechen", e -> close());
        getFooter().add(abbrechen, erzeugen);
    }

    private void starte() {
        String pfad = Kategorie.normalisiere(kategorie.getValue());
        if (pfad.isEmpty()) {
            kategorie.setInvalid(true);
            kategorie.setErrorMessage("Kategorie angeben");
            return;
        }
        Integer n = anzahl.getValue();
        if (n == null || n < 1 || n > KiFragenService.MAX_FRAGEN) {
            anzahl.setInvalid(true);
            anzahl.setErrorMessage("1 bis " + KiFragenService.MAX_FRAGEN);
            return;
        }
        if (modell.getValue() == null) {
            return;
        }
        List<String> adressen = Arrays.stream(links.getValue().split("\\s+"))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        for (String a : adressen) {
            if (!a.matches("(?i)https?://\\S+")) {
                links.setInvalid(true);
                links.setErrorMessage("Keine gültige Adresse: " + a);
                return;
            }
        }
        links.setInvalid(false);
        fehlerText.setVisible(false);

        Auftrag auftrag = new Auftrag(pfad, idee.getValue(), adressen, n, modell.getValue());
        List<QuizItem> bestehende = service.bestehendeFragen(pfad);

        // Warten anzeigen; die KI braucht je nach Umfang einige Minuten (ohne Push: Polling)
        formular.setEnabled(false);
        erzeugen.setEnabled(false);
        ProgressBar balken = new ProgressBar();
        balken.setIndeterminate(true);
        Paragraph warte = new Paragraph("Die KI erzeugt " + n + " Fragen für „" + pfad + "“"
                + (bestehende.isEmpty() ? "" : " und kennt die " + bestehende.size() + " vorhandenen")
                + (adressen.isEmpty() ? "" : ", die Dokumente werden gelesen") + ". Das kann einige Minuten dauern …");
        Div warten = new Div(warte, balken);
        add(warten);

        UI ui = UI.getCurrent();
        ui.setPollInterval(1500);
        CompletableFuture.supplyAsync(() -> service.erzeuge(auftrag, bestehende))
                .whenComplete((vorschlag, fehler) -> ui.access(() -> {
                    ui.setPollInterval(-1);
                    remove(warten);
                    if (fehler != null) {
                        Throwable ursache = fehler.getCause() != null ? fehler.getCause() : fehler;
                        if (!(ursache instanceof KiFehler)) {
                            log.warn("KI-Fragen fehlgeschlagen", ursache);
                        }
                        fehlerText.setText("Fehler: " + ursache.getMessage());
                        fehlerText.setVisible(true);
                        formular.setEnabled(true);
                        erzeugen.setEnabled(true);
                    } else {
                        zeigeVorschlag(pfad, vorschlag);
                    }
                }));
    }

    private void zeigeVorschlag(String pfad, Vorschlag vorschlag) {
        remove(formular, fehlerText);
        getFooter().removeAll();
        setHeaderTitle("Vorschläge für „" + pfad + "“");
        setWidth("min(70em, 96vw)");

        VerticalLayout inhalt = new VerticalLayout();
        inhalt.setPadding(false);
        for (String h : vorschlag.hinweise()) {
            Span s = new Span(h);
            s.getElement().getThemeList().add("badge contrast");
            s.getStyle().set("white-space", "normal");
            inhalt.add(s);
        }

        Grid<KiFrage> grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(LitRenderer.<KiFrage>of("""
                <div style="white-space:normal;padding:var(--lumo-space-xs) 0">
                  <b>${item.frage}</b><br>
                  <span style="color:var(--lumo-success-text-color)">✓ ${item.richtig}</span><br>
                  <span style="color:var(--lumo-secondary-text-color)">✗ ${item.falsch}</span><br>
                  <small style="color:var(--lumo-secondary-text-color)">${item.beschreibung}</small>
                </div>""")
                .withProperty("frage", KiFrage::frage)
                .withProperty("richtig", KiFrage::correctAnswer)
                .withProperty("falsch", f -> String.join("  ·  ",
                        f.antworten().stream().filter(x -> !x.equals(f.correctAnswer())).toList()))
                .withProperty("beschreibung", KiFrage::beschreibung))
                .setHeader("Frage, richtige / falsche Antworten, Erklärung").setFlexGrow(1);
        grid.setItems(vorschlag.fragen());
        grid.asMultiSelect().select(vorschlag.fragen());
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT, GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("min(60vh, 40em)");
        MobilGrid.kompakt(grid);
        grid.setEmptyStateText("Die KI hat keine verwendbaren Fragen geliefert");
        inhalt.add(grid);
        add(inhalt);

        Button importieren = new Button("Auswahl importieren", VaadinIcon.DOWNLOAD_ALT.create());
        importieren.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        importieren.setEnabled(!vorschlag.fragen().isEmpty());
        grid.asMultiSelect().addValueChangeListener(e -> {
            importieren.setEnabled(!e.getValue().isEmpty());
            importieren.setText(anzahlText(e.getValue().size()));
        });
        importieren.setText(anzahlText(vorschlag.fragen().size()));
        importieren.addClickListener(e -> {
            List<KiFrage> auswahl = vorschlag.fragen().stream().filter(grid.getSelectedItems()::contains).toList();
            var ergebnis = service.importiere(pfad, auswahl);
            String text = ergebnis.angelegt() + (ergebnis.angelegt() == 1 ? " Frage" : " Fragen") + " in „" + pfad + "“ angelegt"
                    + (ergebnis.fehler().isEmpty() ? "" : ", " + ergebnis.fehler().size() + " Fehler: "
                            + String.join("; ", ergebnis.fehler()));
            Notification.show(text, 6000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(ergebnis.fehler().isEmpty() ? NotificationVariant.LUMO_SUCCESS
                            : NotificationVariant.LUMO_WARNING);
            close();
            nachImport.run();
        });
        Button verwerfen = new Button("Verwerfen", e -> close());
        getFooter().add(verwerfen, importieren);
    }

    private static String anzahlText(int n) {
        return n == 1 ? "1 Frage importieren" : n + " Fragen importieren";
    }
}
