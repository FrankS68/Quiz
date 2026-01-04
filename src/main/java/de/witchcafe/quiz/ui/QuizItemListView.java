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
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;

import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import jakarta.annotation.security.PermitAll;

@Route("/questionitems")
@PageTitle("QuestionItem List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "QuestionItem")
@PermitAll
class QuizItemListView extends VerticalLayout {

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


	public class UploadDialog extends Dialog{
	    TextField category;
	    Upload upload;
	    
	    public UploadDialog() {
			super();

		    setHeaderTitle("Upload Quiz Items");
	        
	        category = new TextField("Category");
	        category.setPlaceholder("Category");
	        category.setAriaLabel("Category");      
	        
	        InMemoryUploadHandler inMemoryHandler = UploadHandler
	                .inMemory((metadata, data) -> {
	                    // Get other information about the file.
	                    String fileName = metadata.fileName();
	                    String mimeType = metadata.contentType();
	                    long contentLength = metadata.contentLength();

	                    System.out.println(fileName + "\t" + mimeType);
	                    try{
	                    	ArrayList<LinkedTreeMap> importedArray = new Gson().fromJson(new String(data), ArrayList.class);
	                    	System.out.println("ArrayLength: "+importedArray.size());
	                    	importedArray.forEach(importedItem -> {
                				quizItemService.createQuizItem(
	                    				importedItem.get("thema").toString(),
	                    				importedItem.get("frage").toString(), 
	                    				importedItem.get("correct_answer").toString(),
	                    				"",
	                    				(ArrayList<String>) importedItem.get("antworten"),
	                    				new ArrayList<String>());
	                    		System.out.println(importedItem);	
	                    	});
	                    	System.out.println("ArrayLength: "+importedArray.size());
	                    }
	                    catch (Exception exc) {
	                    	System.err.println(exc.getMessage()+"\n"+new String(data));
//	                    	UploadI18N i18N = new UploadI18N();
//	                    	i18N.getUploading().getError().setUnexpectedServerError(
//	                    	        "File couldn't be uploaded, please try again later");
//	                    	upload.setI18n(i18N);	                    	
	                    }
	                    // Do something with the file data...
	                    // processFile(data, fileName);
	                });
	        upload = new Upload(inMemoryHandler);
		    
		    Button closeButton = new Button("Close", e -> close());
	        getFooter().add(closeButton);
	        
	        VerticalLayout dialogLayout = new VerticalLayout(category,upload);
	        dialogLayout.setPadding(false);
	        dialogLayout.setSpacing(false);
	        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");
	        add(dialogLayout);
	    }
	}

	public class EditDialog extends Dialog{
	    TextField category;
	    TextField question;
	    TextField answer;
	    TextArea description;

	    public EditDialog() {
			super();

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
	        description.setMinWidth("20em");

	        VerticalLayout dialogLayout = new VerticalLayout(category,question,answer,description);
	        dialogLayout.setPadding(false);
	        dialogLayout.setSpacing(false);
	        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");

	        setHeaderTitle("New Quiz Item");

	        add(dialogLayout);
			Button saveButton = new Button("Save", e -> {
		    	quizItemService.createQuizItem(category.getValue(),question.getValue(),answer.getValue(),description.getValue());
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

	private void createEditDialog() {
        EditDialog editDialog = new EditDialog();
        editBtn = new Button("Edit", event -> editDialog.open());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add(editDialog, editBtn);
	}
	
	private void createUploadDialog() {
	    Dialog uploadDialog = new UploadDialog();
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
