package de.witchcafe.quiz.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;

public class EditDialog extends Dialog {

    private final QuizItemService quizItemService;
    private final Grid<QuizItem> quizItemGrid;

    private TextField category;
    private TextField question;
    private TextField answer;
    private TextArea description;

    public EditDialog(QuizItemService quizItemService, Grid<QuizItem> quizItemGrid) {
        super();
        this.quizItemService = quizItemService;
        this.quizItemGrid = quizItemGrid;

        category = new TextField("Category");
        category.setPlaceholder("Category");
        category.setAriaLabel("Category");
        
        question = new TextField("Question");
        question.setPlaceholder("Question");
        question.setAriaLabel("Question");
        
        answer = new TextField("Answer");
        answer.setPlaceholder("Answer");
        answer.setAriaLabel("Answer");
        
        description = new TextArea("Description");
        description.setMinRows(4);
        description.setMaxRows(8);
        description.setPlaceholder("What is that quiz item meant for?");
        description.setAriaLabel("QuizItem description");
        description.setMaxLength(QuizItem.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("22em");

        VerticalLayout dialogLayout = new VerticalLayout(category, question, answer, description);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");

        setHeaderTitle("New Quiz Item");

        add(dialogLayout);
        
        Button saveButton = new Button("Save", e -> {
            quizItemService.createQuizItem(
                category.getValue(), 
                question.getValue(), 
                answer.getValue(), 
                description.getValue());
            quizItemGrid.getDataProvider().refreshAll();
            category.clear();
            question.clear();
            answer.clear();
            description.clear();
            Notification.show("QuizItem added", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> close());
        getFooter().add(cancelButton);
        getFooter().add(saveButton);
    }
}
