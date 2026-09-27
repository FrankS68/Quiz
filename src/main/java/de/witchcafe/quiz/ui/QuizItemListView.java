package de.witchcafe.quiz.ui;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("/questionitems")
@PageTitle("QuestionItem List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "QuestionItem")
@AnonymousAllowed
public class QuizItemListView extends VerticalLayout {

    private final QuizItemService quizItemService;

    Button editBtn;
    Button uploadBtn;
    final Grid<QuizItem> quizItemGrid;

    QuizItemListView(QuizItemService quizItemService) {
        this.quizItemService = quizItemService;

        /*
        category = new TextField();
        category.setPlaceholder("Category");
        category.setAriaLabel("Category");
        
        question = new TextField();
        question.setPlaceholder("Question");
        question.setAriaLabel("Question");
        
        answer = new TextField();
        answer.setPlaceholder("Answer");
        answer.setAriaLabel("Answer");
        
        description = new TextField();
        description.setPlaceholder("What is that quiz item meant for?");
        description.setAriaLabel("Role description");
        description.setMaxLength(QuizItem.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("20em");
        */

        // createBtn = new Button("Create", event -> createQuizItem());
        // createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        createEditDialog();
        createUploadDialog();

        quizItemGrid = new Grid<>();
        quizItemGrid.setItems(query -> quizItemService.list(toSpringPageRequest(query)).stream());
        quizItemGrid.addColumn(QuizItem::getCategory).setHeader("Category");
        quizItemGrid.addColumn(QuizItem::getQuestion).setHeader("Question");
        // quizItemGrid.addColumn(QuizItem::getAnswer).setHeader("Answer");
        quizItemGrid.addColumn(QuizItem::getDescription).setHeader("Description");

        quizItemGrid.setEmptyStateText("No Items to be quizzed");
        quizItemGrid.setSizeFull();
        quizItemGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        /*
        add(new ViewToolbar("QuizItem List", 
        //		ViewToolbar.group(category,question,answer,description, 
        		createBtn));
        		*/
        add(quizItemGrid);
    }

    private void createEditDialog() {
        EditDialog editDialog = new EditDialog(quizItemService, quizItemGrid);
        editBtn = new Button("Edit", event -> editDialog.open());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add(editDialog, editBtn);
	}
	
	private void createUploadDialog() {
	    UploadDialog uploadDialog = new UploadDialog(quizItemService, quizItemGrid);
        uploadBtn = new Button("Upload", event -> uploadDialog.open());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(uploadDialog, uploadBtn);
	}

    /*
    private void createQuizItem() {
    	quizItemService.createQuizItem(category.getValue(),question.getValue(),answer.getValue(),description.getValue());
        quizItemGrid.getDataProvider().refreshAll();
        category.clear();
        question.clear();
        answer.clear();
        description.clear();
        Notification.show("Role added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    */

}
