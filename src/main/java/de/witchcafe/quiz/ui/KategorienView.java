package de.witchcafe.quiz.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.quiz.Kategorie;
import de.witchcafe.quiz.KategorieService;
import de.witchcafe.quiz.KategorieService.Info;
import de.witchcafe.quiz.QuizRoles;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.RolesAllowed;

/**
 * Kategorien pflegen (Editoren und Admins): umbenennen bzw. im Baum verschieben
 * (Fragen, Resultate, Schlagworte und Favoriten ziehen mit) und Schlagworte vergeben.
 * Die Hierarchie entsteht aus dem Pfad: "Drohnen/EU A1-A3/Luftrecht".
 */
@Route("kategorien")
@PageTitle("Kategorien | Quiz")
@Menu(order = 1, icon = "vaadin:folder-open-o", title = "Kategorien")
@RolesAllowed({ AppRoles.ADMIN, QuizRoles.EDITOR })
public class KategorienView extends VerticalLayout {

    private final KategorieService kategorieService;
    private final TreeGrid<Info> tree = new TreeGrid<>();
    private final TextField suche = new TextField();
    private List<Info> alle = List.of();

    KategorienView(KategorieService kategorieService) {
        this.kategorieService = kategorieService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        suche.setPlaceholder("Kategorie oder Schlagwort suchen");
        suche.setPrefixComponent(VaadinIcon.SEARCH.create());
        suche.setClearButtonVisible(true);
        suche.setValueChangeMode(ValueChangeMode.LAZY);
        suche.addValueChangeListener(e -> KategorieBaum.fuelle(tree, alle, e.getValue()));
        suche.setWidthFull();
        suche.setMaxWidth("28em");

        tree.addHierarchyColumn(Info::name).setHeader("Kategorie").setFlexGrow(1).setTooltipGenerator(Info::pfad);
        var schlagwortSpalte = tree.addColumn(Info::schlagworte).setHeader("Schlagworte").setFlexGrow(2);
        var fragenSpalte = tree.addColumn(i -> i.eigeneFragen() == i.fragenGesamt()
                        ? String.valueOf(i.fragenGesamt())
                        : i.eigeneFragen() + " / " + i.fragenGesamt())
                .setHeader("Fragen (eigene / mit Unterbereichen)").setAutoWidth(true).setFlexGrow(0);
        tree.addComponentColumn(i -> {
            Button bearbeiten = new Button(VaadinIcon.EDIT.create(), e -> bearbeite(i));
            bearbeiten.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            bearbeiten.setTooltipText("Umbenennen, verschieben, Schlagworte");
            bearbeiten.setAriaLabel("Bearbeiten");
            return bearbeiten;
        }).setAutoWidth(true).setFlexGrow(0);
        Responsive.hideOnMobile(tree, schlagwortSpalte, fragenSpalte);
        tree.addItemDoubleClickListener(e -> bearbeite(e.getItem()));
        tree.setEmptyStateText("Keine Kategorien");
        tree.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        tree.setSizeFull();

        Paragraph hinweis = new Paragraph("Ebenen mit „/“ trennen, z. B. „Drohnen/EU A1-A3/Luftrecht“ "
                + "(Oberthema / Zertifikat / Abschnitt). Wer eine Kategorie umbenennt oder verschiebt, nimmt alle "
                + "Fragen, Unterbereiche, Resultate, Schlagworte und Favoriten mit.");
        hinweis.getStyle().setColor("var(--lumo-secondary-text-color)").setFontSize("var(--lumo-font-size-s)")
                .setMargin("0");

        VerticalLayout kopf = new VerticalLayout(hinweis, suche);
        kopf.setPadding(true);
        kopf.getStyle().setPaddingTop("0");

        add(new ViewToolbar("Kategorien"), kopf, tree);
        laden();
    }

    private void laden() {
        alle = kategorieService.alle();
        KategorieBaum.fuelle(tree, alle, suche.getValue());
    }

    private void bearbeite(Info i) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kategorie bearbeiten");

        TextField pfad = new TextField("Pfad");
        pfad.setValue(i.pfad());
        pfad.setHelperText("Umbenennen oder verschieben, Ebenen mit / trennen");
        pfad.setMaxLength(Kategorie.PFAD_MAX_LENGTH);
        pfad.setWidthFull();

        TextArea schlagworte = new TextArea("Schlagworte");
        schlagworte.setValue(i.schlagworte());
        schlagworte.setHelperText("Durch Komma getrennt, z. B. „Drohnenführerschein, UAS, Open Category“");
        schlagworte.setMaxLength(Kategorie.SCHLAGWORTE_MAX_LENGTH);
        schlagworte.setWidthFull();

        VerticalLayout form = new VerticalLayout(pfad, schlagworte);
        form.setPadding(false);
        dialog.add(form);

        Button speichern = new Button("Speichern", e -> {
            String neu = Kategorie.normalisiere(pfad.getValue());
            if (neu.isEmpty()) {
                pfad.setInvalid(true);
                pfad.setErrorMessage("Pfad darf nicht leer sein");
                return;
            }
            try {
                // erst verschieben, dann die Schlagworte am neuen Pfad speichern
                int verschoben = kategorieService.verschiebe(i.pfad(), neu);
                kategorieService.speichereSchlagworte(neu, schlagworte.getValue());
                dialog.close();
                laden();
                Notification.show(neu.equals(i.pfad()) ? "Gespeichert"
                                : "Verschoben nach „" + neu + "“ (" + verschoben + " Fragen)",
                        3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                pfad.setInvalid(true);
                pfad.setErrorMessage(ex.getMessage());
            }
        });
        speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button abbrechen = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(new HorizontalLayout(abbrechen, speichern));
        Responsive.dialog(dialog, "32em");
        dialog.open();
    }
}
