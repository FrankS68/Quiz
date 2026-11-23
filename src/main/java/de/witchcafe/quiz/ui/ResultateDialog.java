package de.witchcafe.quiz.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;

import de.witchcafe.auth.domain.AppUser;
import de.witchcafe.quiz.Quiz;
import de.witchcafe.quiz.QuizService;
import de.witchcafe.ui.Responsive;

/** Gelaufene Quizze einer Kategorie inkl. Unterbereiche, neueste zuerst (vorerst alle Benutzer). */
public class ResultateDialog extends Dialog {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Berlin"));

    public ResultateDialog(String category, QuizService quizService) {
        setHeaderTitle("Resultate: " + category.replace("/", " › "));

        List<Quiz> resultate = quizService.resultate(category);
        Grid<Quiz> grid = new Grid<>();
        grid.setItems(resultate);
        grid.addColumn(q -> q.getGespieltAm() == null ? "–" : FMT.format(q.getGespieltAm()))
                .setHeader("Gespielt am").setAutoWidth(true);
        grid.addComponentColumn(q -> {
            AppUser user = q.getUser();
            Span span = new Span(user == null ? "Gast" : user.getName());
            if (user != null) {
                span.setTitle(user.getProvider());
            }
            return span;
        }).setHeader("Benutzer").setAutoWidth(true);
        grid.addColumn(q -> q.getCategory().equals(category) ? "" : q.getCategory().substring(category.length() + 1).replace("/", " › "))
                .setHeader("Bereich").setAutoWidth(true);
        grid.addColumn(q -> anzahlFragen(q.getResult())).setHeader("Fragen").setAutoWidth(true);
        grid.setEmptyStateText("In dieser Kategorie wurde noch kein Quiz gespielt");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(resultate.size() <= 10);
        if (resultate.size() > 10) {
            grid.setHeight("24em");
        }
        add(grid);

        getFooter().add(new Button("Schliessen", e -> close()));
        Responsive.dialog(this, "36em");
    }

    /** Neue Eintraege speichern die IDs der gespielten Fragen als JSON-Array, alte nur "{}". */
    private static String anzahlFragen(String result) {
        if (result == null || !result.trim().startsWith("[")) {
            return "–";
        }
        try {
            return String.valueOf(new Gson().fromJson(result, List.class).size());
        } catch (RuntimeException e) {
            return "–";
        }
    }
}
