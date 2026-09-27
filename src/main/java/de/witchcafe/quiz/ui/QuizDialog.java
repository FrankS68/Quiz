package de.witchcafe.quiz.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import consulting.segieth.security.SecurityService;
import consulting.segieth.security.UserProfile;
import de.witchcafe.quiz.Quiz;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizService;

public class QuizDialog extends Dialog {

    private final QuizService quizService;
    private final QuizItemService quizItemService;
    private final SecurityService securityService;
    private final Grid<Quiz> quizGrid;

    private HashMap<QuizItem, String> result = new HashMap<>();
    private List<QuizItem> quizItems;
    private QuizItem quizItem;
    private Span questionSpan;
    private Span answerSpan;
    private Integer quizIndex = 0;

    public QuizDialog(String category, QuizService quizService, 
                     QuizItemService quizItemService, SecurityService securityService,
                     Grid<Quiz> quizGrid) {
        this.quizService = quizService;
        this.quizItemService = quizItemService;
        this.securityService = securityService;
        this.quizGrid = quizGrid;

        quizItems = quizItemService.findByCategory(category);
        Collections.shuffle(quizItems);
        if (quizItems.size() > 5) {
            quizItems = quizItems.subList(0, 5);
        }
        
        quizItem = quizItems.get(quizIndex);
        
        Button endButton = new Button("End", e -> {
            quizService.createQuiz(
                category,
                securityService.getUserProfile(),
                new Gson().toJson(result));
            quizGrid.getDataProvider().refreshAll();
            Notification.show("Quiz added", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
        });
        
        Button closeButton = new Button("Close", e -> close());
        getFooter().add(endButton, closeButton);
        getHeader().add(category);
        
        questionSpan = new Span(quizItem.getQuestion());
        questionSpan.addClickListener(e -> {
            answerSpan.setVisible(true);
            answerSpan.setEnabled(true);
        });
        
        answerSpan = new Span(quizItem.getAnswer());
        answerSpan.setVisible(false);
        answerSpan.setEnabled(false);
        answerSpan.addClickListener(e -> {
            answerSpan.setVisible(false);
            answerSpan.setEnabled(false);
            if (++quizIndex >= quizItems.size()) {
                quizService.createQuiz(
                    category,
                    securityService.getUserProfile(),
                    new Gson().toJson(result));
                quizGrid.getDataProvider().refreshAll();
                Notification.show("Quiz added", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                close();
            }
            
            quizItem = quizItems.get(quizIndex);
            questionSpan.setText(quizItem.getQuestion());
            answerSpan.setText(quizItem.getAnswer());
        });
        
        VerticalLayout dialogLayout = new VerticalLayout(questionSpan, answerSpan);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");
        add(dialogLayout);
    }
}
