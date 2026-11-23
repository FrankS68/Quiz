package de.witchcafe.quiz.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.billing.client.InsufficientCreditException;
import de.witchcafe.billing.client.api.BookingResponse;
import de.witchcafe.quiz.QuizBilling;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizService;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.PermitAll;

/**
 * Startseite fuer alle Benutzer: die Kategorien als Liste bzw. Baum, je Kategorie
 * "Starten" (Quiz spielen) und "Resultate" (gelaufene Quizze dieser Kategorie).
 * Kategorien mit '/' im Namen (z.B. "Drohnen/Luftrecht") werden als Baum verschachtelt.
 */
@Route("/quizzes")
@RouteAlias("") // Startseite (Ziel nach dem witch-auth-Login)
@PageTitle("Quiz")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Quiz")
@PermitAll
public class QuizListView extends VerticalLayout {

    /**
     * Knoten im Kategorienbaum. {@code kategorie} ist der volle Name wie in der Datenbank,
     * {@code null} bei reinen Gruppen (es gibt keine Fragen direkt unter diesem Pfad).
     */
    record Knoten(String pfad, String name, @Nullable String kategorie, long anzahl) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Knoten k && k.pfad.equals(pfad);
        }

        @Override
        public int hashCode() {
            return pfad.hashCode();
        }
    }

    private final QuizService quizService;
    private final QuizItemService quizItemService;
    private final CurrentUser currentUser;
    private final QuizBilling quizBilling;
    private final Span balanceSpan = new Span();
    private final TreeGrid<Knoten> tree = new TreeGrid<>();

    QuizListView(QuizService quizService, QuizItemService quizItemService,
                 CurrentUser currentUser, QuizBilling quizBilling) {
        this.quizService = quizService;
        this.quizItemService = quizItemService;
        this.currentUser = currentUser;
        this.quizBilling = quizBilling;

        tree.addHierarchyColumn(Knoten::name).setHeader("Kategorie").setFlexGrow(1);
        tree.addColumn(k -> k.anzahl() > 0 ? String.valueOf(k.anzahl()) : "")
                .setHeader("Fragen").setAutoWidth(true).setFlexGrow(0);
        tree.addComponentColumn(this::aktionen).setAutoWidth(true).setFlexGrow(0);
        tree.setEmptyStateText("Noch keine Kategorien - es wurden noch keine Fragen angelegt");
        tree.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        tree.setSizeFull();
        ladeKategorien();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        // Guthaben aus witch-billing; unsichtbar, wenn Billing aus oder nicht erreichbar
        balanceSpan.getElement().getThemeList().add("badge");
        balanceSpan.setTitle("Dein Guthaben (gilt für alle WitchCafe-Apps)");
        balanceSpan.setVisible(false);
        quizBilling.balance().ifPresent(this::showBalance);

        add(new ViewToolbar("Quiz", ViewToolbar.group(balanceSpan)));
        add(tree);
    }

    private HorizontalLayout aktionen(Knoten k) {
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        if (k.kategorie() == null) {
            return buttons;
        }
        Button starten = new Button("Starten", VaadinIcon.PLAY.create(), e -> startQuiz(k.kategorie()));
        starten.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        Button resultate = new Button("Resultate", VaadinIcon.LIST.create(),
                e -> new ResultateDialog(k.kategorie(), quizService).open());
        resultate.addThemeVariants(ButtonVariant.LUMO_SMALL);
        buttons.add(starten, resultate);
        return buttons;
    }

    /** Baut den Baum aus den Kategorienamen; '/' trennt die Ebenen. */
    private void ladeKategorien() {
        Map<String, Long> anzahl = new LinkedHashMap<>();
        quizItemService.lookupCategories().stream()
                .sorted((a, b) -> a[0].toString().compareToIgnoreCase(b[0].toString()))
                .forEach(r -> anzahl.put(r[0].toString(), ((Number) r[1]).longValue()));

        TreeData<Knoten> daten = new TreeData<>();
        Map<String, Knoten> knoten = new LinkedHashMap<>();
        for (String kategorie : anzahl.keySet()) {
            String[] teile = kategorie.split("/");
            Knoten eltern = null;
            StringBuilder pfad = new StringBuilder();
            for (int i = 0; i < teile.length; i++) {
                String name = teile[i].trim();
                if (name.isEmpty()) {
                    continue;
                }
                if (pfad.length() > 0) {
                    pfad.append('/');
                }
                pfad.append(name);
                String p = pfad.toString();
                // Die Kategorie selbst oder eine Gruppe, die zugleich eine Kategorie ist
                String eigene = anzahl.containsKey(p) ? p : (i == teile.length - 1 ? kategorie : null);
                Knoten k = knoten.get(p);
                if (k == null) {
                    k = new Knoten(p, name, eigene, eigene == null ? 0 : anzahl.getOrDefault(eigene, 0L));
                    knoten.put(p, k);
                    daten.addItem(eltern, k);
                }
                eltern = k;
            }
        }
        tree.setDataProvider(new TreeDataProvider<>(daten));
        // Gruppen aufgeklappt zeigen
        List<Knoten> gruppen = new ArrayList<>(knoten.values().stream()
                .filter(k -> !daten.getChildren(k).isEmpty()).toList());
        tree.expand(gruppen);
    }

    private void startQuiz(String category) {
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
        new QuizDialog(Objects.requireNonNull(category), quizService, quizItemService, currentUser, () -> { }).open();
    }

    private void showBalance(long credits) {
        balanceSpan.setText(credits + " Credits");
        balanceSpan.setVisible(true);
    }
}
