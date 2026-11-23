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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.spring.security.AuthenticationContext;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizRoles;
import de.witchcafe.ui.Responsive;
import jakarta.annotation.security.RolesAllowed;

/**
 * Fragenkatalog - nur fuer Editoren und Admins. Einfache Benutzer (USER) sehen nur die
 * Quiz-Ebene (QuizListView): Quiz spielen und gelaufene Quizze ansehen. Vaadin blendet die
 * Seite fuer sie auch im Menue aus.
 */
@Route("/questionitems")
@PageTitle("QuestionItem List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "QuestionItem")
@RolesAllowed({AppRoles.ADMIN, QuizRoles.EDITOR})
public class QuizItemListView extends VerticalLayout {

    private final QuizItemService quizItemService;

    Button editBtn;
    Button uploadBtn;
    final Grid<QuizItem> quizItemGrid;

    QuizItemListView(QuizItemService quizItemService, AuthenticationContext authenticationContext) {
        this.quizItemService = quizItemService;
        // Bearbeiten und Hochladen nur fuer Editoren und Admins (siehe auch QuizItemService)
        boolean canEdit = authenticationContext.hasAnyRole(AppRoles.ADMIN, QuizRoles.EDITOR);

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

        quizItemGrid = new Grid<>();
        HorizontalLayout aktionen = new HorizontalLayout();
        if (canEdit) {
            createEditDialog();
            createUploadDialog();
            aktionen.add(editBtn, uploadBtn);
        }
        quizItemGrid.setItems(query -> quizItemService.list(toSpringPageRequest(query)).stream());
        quizItemGrid.addColumn(QuizItem::getCategory).setHeader("Category");
        quizItemGrid.addColumn(QuizItem::getQuestion).setHeader("Question");
        // quizItemGrid.addColumn(QuizItem::getAnswer).setHeader("Answer");
        var descriptionColumn = quizItemGrid.addColumn(QuizItem::getDescription).setHeader("Description");
        // Bild: Symbol, wenn gesetzt; Editoren oeffnen per Klick den Bild-Dialog
        quizItemGrid.addComponentColumn(item -> {
            Button b = new Button((item.hatBild() ? VaadinIcon.PICTURE : VaadinIcon.PLUS_CIRCLE_O).create());
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            b.setTooltipText(item.hatBild() ? "Bild ändern" : "Bild hinzufügen");
            b.setAriaLabel(b.getTooltip().getText());
            b.setEnabled(canEdit);
            b.addClickListener(e -> new BildDialog(item, quizItemService,
                    () -> quizItemGrid.getDataProvider().refreshAll()).open());
            return b;
        }).setHeader("Bild").setAutoWidth(true).setFlexGrow(0);
        // Auf dem Handy nur Kategorie und Frage
        Responsive.hideOnMobile(quizItemGrid, descriptionColumn);

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
        if (canEdit) {
            aktionen.setPadding(true);
            add(Responsive.wrappingRow(aktionen));
        }
        add(quizItemGrid);
    }

    private void createEditDialog() {
        EditDialog editDialog = new EditDialog(quizItemService, quizItemGrid);
        editBtn = new Button("Edit", event -> editDialog.open());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add(editDialog);
	}
	
	private void createUploadDialog() {
	    UploadDialog uploadDialog = new UploadDialog(quizItemService, quizItemGrid);
        uploadBtn = new Button("Upload", event -> uploadDialog.open());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(uploadDialog);
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
