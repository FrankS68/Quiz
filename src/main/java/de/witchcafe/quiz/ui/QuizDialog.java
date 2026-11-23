package de.witchcafe.quiz.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizService;
import de.witchcafe.ui.Responsive;

/**
 * Spielt bis zu 5 zufaellige Fragen einer Kategorie inkl. ihrer Unterbereiche: Klick auf die Frage zeigt die Antwort,
 * Klick auf die Antwort fuehrt zur naechsten Frage. Am Ende (oder mit "End") wird das Quiz
 * gespeichert; {@code result} enthaelt die IDs der gespielten Fragen als JSON-Array.
 */
public class QuizDialog extends Dialog {

    private final List<Long> gespielt = new ArrayList<>();
    private final List<QuizItem> quizItems;
    private int quizIndex = 0;
    private boolean gespeichert = false;

    private final Span questionSpan;
    private final Span answerSpan;

    public QuizDialog(String category, QuizService quizService,
                      QuizItemService quizItemService, CurrentUser currentUser,
                      Runnable nachSpeichern) {

        List<QuizItem> alle = new ArrayList<>(quizItemService.findImTeilbaum(category));
        Collections.shuffle(alle);
        quizItems = alle.size() > 5 ? alle.subList(0, 5) : alle;

        Runnable speichern = () -> {
            if (gespeichert) {
                return;
            }
            gespeichert = true;
            quizService.createQuiz(category, currentUser.get().orElse(null), new Gson().toJson(gespielt));
            nachSpeichern.run();
            Notification.show("Quiz gespeichert", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
        };

        Button endButton = new Button("End", e -> speichern.run());
        Button closeButton = new Button("Close", e -> close());
        getFooter().add(endButton, closeButton);
        getHeader().add(category.replace("/", " › "));

        questionSpan = new Span();
        answerSpan = new Span();
        answerSpan.setVisible(false);

        questionSpan.addClickListener(e -> {
            if (!quizItems.isEmpty()) {
                answerSpan.setVisible(true);
            }
        });
        answerSpan.addClickListener(e -> {
            answerSpan.setVisible(false);
            gespielt.add(quizItems.get(quizIndex).getId());
            if (++quizIndex >= quizItems.size()) {
                speichern.run();
                return;
            }
            zeigeFrage();
        });

        if (quizItems.isEmpty()) {
            questionSpan.setText("In dieser Kategorie gibt es noch keine Fragen.");
            endButton.setVisible(false);
        } else {
            zeigeFrage();
        }

        VerticalLayout dialogLayout = new VerticalLayout(questionSpan, answerSpan);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        add(dialogLayout);
        Responsive.dialog(this, "28em");
    }

    private void zeigeFrage() {
        QuizItem item = quizItems.get(quizIndex);
        questionSpan.setText(item.getQuestion());
        answerSpan.setText(item.getAnswer());
    }
}
