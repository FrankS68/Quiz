package de.witchcafe.quiz.ui;

import java.util.ArrayList;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;

public class UploadDialog extends Dialog {

    private final QuizItemService quizItemService;
    private final Grid<QuizItem> quizItemGrid;

    private TextField category;
    private Upload upload;

    public UploadDialog(QuizItemService quizItemService, Grid<QuizItem> quizItemGrid) {
        super();
        this.quizItemService = quizItemService;
        this.quizItemGrid = quizItemGrid;

        setHeaderTitle("Upload Quiz Items");
        
        category = new TextField("Category");
        category.setPlaceholder("Category");
        category.setAriaLabel("Category");
        
        MemoryBuffer memoryBuffer = new MemoryBuffer();
        upload = new Upload(memoryBuffer);
        
        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();
            long contentLength = event.getContentLength();

            System.out.println(fileName + "\t" + mimeType);
            try {
                byte[] data = memoryBuffer.getFileData().toBytes();
                ArrayList<LinkedTreeMap> importedArray = new Gson().fromJson(new String(data), ArrayList.class);
                System.out.println("ArrayLength: " + importedArray.size());
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
                System.out.println("ArrayLength: " + importedArray.size());
                quizItemGrid.getDataProvider().refreshAll();
            }
            catch (Exception exc) {
                System.err.println(exc.getMessage());
            }
        });
        
        Button closeButton = new Button("Close", e -> close());
        getFooter().add(closeButton);
        
        VerticalLayout dialogLayout = new VerticalLayout(category, upload);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");
        add(dialogLayout);
    }
}
