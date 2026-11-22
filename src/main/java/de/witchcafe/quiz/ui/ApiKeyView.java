package de.witchcafe.quiz.ui;

import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.quiz.QuizRoles;
import de.witchcafe.quiz.api.ApiKey;
import de.witchcafe.quiz.api.ApiKeyService;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.RolesAllowed;

/**
 * API-Keys fuer die REST-API verwalten (analog knownet). EDITOR darf Fragen importieren,
 * USER nur lesen. Der Token wird nur einmal beim Erzeugen angezeigt.
 */
@Route("admin/apikeys")
@Menu(order = 98, icon = "vaadin:key-o", title = "API-Keys")
@PageTitle("API-Keys | Quiz")
@RolesAllowed(AppRoles.ADMIN)
public class ApiKeyView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ApiKeyService apiKeyService;
    private final Grid<ApiKey> grid = new Grid<>(ApiKey.class, false);

    public ApiKeyView(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
        setSizeFull();
        setPadding(false);

        Button neuerKey = new Button("Neuer API-Key", e -> oeffneNeuDialog());
        neuerKey.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addColumn(ApiKey::getName).setHeader("Name").setAutoWidth(true);
        var rolleSpalte = grid.addColumn(ApiKey::getRolle).setHeader("Rolle").setAutoWidth(true);
        var erstelltSpalte = grid.addColumn(k -> FMT.format(k.getErstelltAm())).setHeader("Erstellt").setAutoWidth(true);
        grid.addComponentColumn(k -> {
            Span badge = new Span(k.isAktiv() ? "Aktiv" : "Deaktiviert");
            badge.getElement().getThemeList().add(k.isAktiv() ? "badge success" : "badge error");
            return badge;
        }).setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::aktionen).setHeader("Aktionen").setAutoWidth(true);

        grid.setSizeFull();
        Responsive.hideOnMobile(grid, rolleSpalte, erstelltSpalte);
        add(new ViewToolbar("API-Keys", neuerKey), grid);
        aktualisiere();
    }

    private HorizontalLayout aktionen(ApiKey k) {
        HorizontalLayout aktionen = new HorizontalLayout();
        aktionen.setWrap(true);
        if (k.isAktiv()) {
            Button deaktivieren = new Button("Deaktivieren", e -> {
                apiKeyService.deaktiviere(k.getId());
                aktualisiere();
                Notification.show("Key deaktiviert");
            });
            deaktivieren.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            aktionen.add(deaktivieren);
        }
        Button loeschen = new Button("Löschen", e -> {
            ConfirmDialog d = new ConfirmDialog();
            d.setHeader("API-Key löschen?");
            d.setText("\"" + k.getName() + "\" wird unwiderruflich gelöscht.");
            d.setCancelable(true);
            d.setCancelText("Abbrechen");
            d.setConfirmText("Löschen");
            d.setConfirmButtonTheme("error primary");
            d.addConfirmListener(ev -> {
                apiKeyService.loesche(k.getId());
                aktualisiere();
                Notification.show("Key gelöscht");
            });
            d.open();
        });
        loeschen.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        aktionen.add(loeschen);
        return aktionen;
    }

    private void oeffneNeuDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neuer API-Key");

        TextField nameFeld = new TextField("Name (z.B. Claude-Agent)");
        nameFeld.setWidthFull();

        Select<String> rolleFeld = new Select<>();
        rolleFeld.setLabel("Rolle");
        rolleFeld.setItems(QuizRoles.EDITOR, AppRoles.USER, AppRoles.ADMIN);
        rolleFeld.setItemLabelGenerator(r -> switch (r) {
            case QuizRoles.EDITOR -> "EDITOR – Fragen lesen und importieren";
            case AppRoles.USER -> "USER – nur lesen";
            default -> r;
        });
        rolleFeld.setValue(QuizRoles.EDITOR);
        rolleFeld.setWidthFull();

        FormLayout form = new FormLayout(nameFeld, rolleFeld);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button erzeugen = new Button("Erzeugen", e -> {
            if (nameFeld.isEmpty()) {
                Notification.show("Bitte einen Namen angeben");
                return;
            }
            String name = nameFeld.getValue().trim();
            String token = apiKeyService.erzeugeApiKey(name, rolleFeld.getValue());
            dialog.close();
            aktualisiere();
            zeigeToken(token, name);
        });
        erzeugen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), erzeugen);
        Responsive.dialog(dialog, "28em");
        dialog.open();
    }

    private void zeigeToken(String token, String name) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("API-Key für \"" + name + "\"");
        dialog.setCloseOnOutsideClick(false);

        TextField tokenFeld = new TextField("Token – nur jetzt sichtbar, bitte sofort kopieren!");
        tokenFeld.setValue(token);
        tokenFeld.setWidthFull();
        tokenFeld.setReadOnly(true);

        Button kopieren = new Button("Kopieren", e -> {
            tokenFeld.getElement().executeJs("navigator.clipboard.writeText($0)", token);
            Notification.show("Token kopiert!", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        kopieren.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(tokenFeld);
        dialog.getFooter().add(kopieren, new Button("Verstanden, schließen", e -> dialog.close()));
        Responsive.dialog(dialog, "36em");
        dialog.open();
    }

    private void aktualisiere() {
        grid.setItems(apiKeyService.alleKeys());
    }
}
