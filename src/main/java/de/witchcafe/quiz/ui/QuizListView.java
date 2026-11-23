package de.witchcafe.quiz.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.billing.client.InsufficientCreditException;
import de.witchcafe.billing.client.api.BookingResponse;
import de.witchcafe.quiz.KategorieService;
import de.witchcafe.quiz.KategorieService.Info;
import de.witchcafe.quiz.QuizBilling;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizModus;
import de.witchcafe.quiz.QuizService;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.PermitAll;

/**
 * Startseite fuer alle Benutzer: oben die Favoriten, darunter die Kategorien als Baum
 * (Oberthema / Zertifikat / Abschnitt, Ebenen getrennt durch '/'), durchsuchbar nach
 * Name und Schlagworten. Je Kategorie "Starten" (Fragen der Kategorie inkl. aller
 * Unterbereiche), "Resultate" und ein Stern fuer die Favoriten.
 */
@Route("/quizzes")
@RouteAlias("") // Startseite (Ziel nach dem witch-auth-Login)
@PageTitle("Quiz")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Quiz")
@PermitAll
public class QuizListView extends VerticalLayout {

    private final QuizService quizService;
    private final QuizItemService quizItemService;
    private final KategorieService kategorieService;
    private final CurrentUser currentUser;
    private final QuizBilling quizBilling;

    private final Span balanceSpan = new Span();
    private final TextField suche = new TextField();
    private final TreeGrid<Info> tree = new TreeGrid<>();
    private final Div favoritenBox = new Div();

    private List<Info> alle = List.of();
    private final Set<String> favoriten = new HashSet<>();
    private boolean mobil = false;

    QuizListView(QuizService quizService, QuizItemService quizItemService, KategorieService kategorieService,
                 CurrentUser currentUser, QuizBilling quizBilling) {
        this.quizService = quizService;
        this.quizItemService = quizItemService;
        this.kategorieService = kategorieService;
        this.currentUser = currentUser;
        this.quizBilling = quizBilling;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        // Guthaben aus witch-billing; unsichtbar, wenn Billing aus oder nicht erreichbar
        balanceSpan.getElement().getThemeList().add("badge");
        balanceSpan.setTitle("Dein Guthaben (gilt für alle WitchCafe-Apps)");
        balanceSpan.setVisible(false);
        quizBilling.balance().ifPresent(this::showBalance);

        suche.setPlaceholder("Kategorie oder Schlagwort suchen");
        suche.setPrefixComponent(VaadinIcon.SEARCH.create());
        suche.setClearButtonVisible(true);
        suche.setValueChangeMode(ValueChangeMode.LAZY);
        suche.addValueChangeListener(e -> KategorieBaum.fuelle(tree, alle, e.getValue()));
        suche.setWidthFull();
        suche.setMaxWidth("28em");

        tree.addHierarchyColumn(Info::name).setHeader("Kategorie").setFlexGrow(1)
                .setTooltipGenerator(Info::pfad);
        var schlagwortSpalte = tree.addColumn(Info::schlagworte).setHeader("Schlagworte").setFlexGrow(1);
        tree.addColumn(i -> String.valueOf(i.fragenGesamt())).setHeader("Fragen").setAutoWidth(true).setFlexGrow(0);
        tree.addComponentColumn(this::aktionen).setAutoWidth(true).setFlexGrow(0);
        Responsive.hideOnMobile(tree, schlagwortSpalte);
        tree.setEmptyStateText("Keine passende Kategorie");
        tree.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        tree.setSizeFull();
        Responsive.onMobileChange(tree, m -> {
            mobil = m;
            tree.getDataProvider().refreshAll();
            zeigeFavoriten();
        });

        favoritenBox.getStyle().setPadding("0 var(--lumo-space-m)");

        HorizontalLayout suchzeile = new HorizontalLayout(suche);
        suchzeile.setWidthFull();
        suchzeile.setPadding(true);
        suchzeile.getStyle().setPaddingTop("0");

        add(new ViewToolbar("Quiz", ViewToolbar.group(balanceSpan)), favoritenBox, suchzeile, tree);
        laden();
    }

    private void laden() {
        alle = kategorieService.alle();
        favoriten.clear();
        userId().ifPresent(id -> favoriten.addAll(kategorieService.favoriten(id)));
        KategorieBaum.fuelle(tree, alle, suche.getValue());
        zeigeFavoriten();
    }

    /** Favoriten als Kacheln oben; ausgeblendet, wenn keine gesetzt sind. */
    private void zeigeFavoriten() {
        favoritenBox.removeAll();
        Map<String, Info> nachPfad = alle.stream().collect(Collectors.toMap(Info::pfad, Function.identity()));
        List<Info> favs = favoriten.stream().sorted().map(nachPfad::get).filter(i -> i != null).toList();
        favoritenBox.setVisible(!favs.isEmpty());
        if (favs.isEmpty()) {
            return;
        }
        H2 titel = new H2("Favoriten");
        titel.getStyle().setFontSize("var(--lumo-font-size-m)").setMargin("var(--lumo-space-s) 0");
        HorizontalLayout kacheln = new HorizontalLayout();
        kacheln.getStyle().set("flex-wrap", "wrap");
        for (Info i : favs) {
            kacheln.add(kachel(i));
        }
        favoritenBox.add(titel, kacheln);
    }

    private Div kachel(Info i) {
        Span name = new Span(i.name());
        name.getStyle().setFontWeight("600");
        Span pfad = new Span(i.eltern() == null ? i.fragenGesamt() + " Fragen"
                : i.eltern().replace("/", " › ") + " · " + i.fragenGesamt() + " Fragen");
        pfad.getStyle().setFontSize("var(--lumo-font-size-s)").setColor("var(--lumo-secondary-text-color)");
        VerticalLayout text = new VerticalLayout(name, pfad);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout zeile = new HorizontalLayout(text, aktionen(i));
        zeile.setAlignItems(FlexComponent.Alignment.CENTER);
        zeile.setSpacing(true);

        Div kachel = new Div(zeile);
        kachel.getStyle()
                .setBorder("1px solid var(--lumo-contrast-10pct)")
                .setBorderRadius("var(--lumo-border-radius-l)")
                .setPadding("var(--lumo-space-s) var(--lumo-space-m)")
                .setBackground("var(--lumo-base-color)");
        kachel.getElement().setAttribute("title", i.pfad());
        return kachel;
    }

    private HorizontalLayout aktionen(Info i) {
        Button starten = new Button(VaadinIcon.PLAY.create(), e -> startQuiz(i));
        starten.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        starten.setTooltipText(i.eigeneFragen() == i.fragenGesamt()
                ? "Quiz starten" : "Quiz mit Fragen aus allen Unterbereichen starten");
        starten.setEnabled(i.fragenGesamt() > 0);

        Button resultate = new Button(VaadinIcon.LIST.create(),
                e -> new ResultateDialog(i.pfad(), quizService).open());
        resultate.addThemeVariants(ButtonVariant.LUMO_SMALL);
        resultate.setTooltipText("Resultate dieser Kategorie");
        if (!mobil) {
            starten.setText("Starten");
            resultate.setText("Resultate");
        }

        boolean fav = favoriten.contains(i.pfad());
        Icon stern = (fav ? VaadinIcon.STAR : VaadinIcon.STAR_O).create();
        if (fav) {
            stern.setColor("var(--lumo-warning-color, #f0b400)");
        }
        Button favorit = new Button(stern, e -> wechsleFavorit(i));
        favorit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        favorit.setTooltipText(fav ? "Aus den Favoriten entfernen" : "Als Favorit merken");
        favorit.setAriaLabel(favorit.getTooltip().getText());
        favorit.setVisible(userId().isPresent());

        HorizontalLayout buttons = new HorizontalLayout(starten, resultate, favorit);
        buttons.setSpacing(true);
        buttons.setAlignItems(FlexComponent.Alignment.CENTER);
        return buttons;
    }

    private void wechsleFavorit(Info i) {
        userId().ifPresent(id -> {
            if (kategorieService.wechsleFavorit(id, i.pfad())) {
                favoriten.add(i.pfad());
            } else {
                favoriten.remove(i.pfad());
            }
            tree.getDataProvider().refreshItem(i);
            zeigeFavoriten();
        });
    }

    private java.util.Optional<String> userId() {
        return currentUser.get().map(u -> u.getUserId().toString());
    }

    private void startQuiz(Info i) {
        new ModusDialog(i.pfad(), modus -> startQuiz(i, modus)).open();
    }

    private void startQuiz(Info i, QuizModus modus) {
        // Runde ueber witch-billing buchen (Preis aus der Preisliste, quiz/spiel)
        try {
            BookingResponse booking = quizBilling.chargeRound();
            if (booking.isBooked()) {
                showBalance(booking.balanceAfter());
            }
        } catch (InsufficientCreditException e) {
            Notification.show("Nicht genug Guthaben: " + e.getBalance() + " Credits, nötig sind "
                    + e.getRequired() + ". Neue Credits gibt es für Beiträge, z.B. neue Fragen.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            showBalance(e.getBalance());
            return;
        }
        QuizLaufDialog.fuer(modus, i.pfad(), quizService, quizItemService, currentUser).open();
    }

    private void showBalance(long credits) {
        balanceSpan.setText(credits + " Credits");
        balanceSpan.setVisible(true);
    }
}
