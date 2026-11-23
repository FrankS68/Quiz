package de.witchcafe.quiz.ui;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import de.witchcafe.quiz.FrageImport;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizItemService.ImportErgebnis;
import de.witchcafe.ui.Responsive;

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
                InputStream inputStream = memoryBuffer.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(dataBuffer)) != -1) {
                    buffer.write(dataBuffer, 0, bytesRead);
                }
                byte[] data = buffer.toByteArray();
                
                ArrayList<LinkedTreeMap> importedArray = new Gson().fromJson(new String(data, java.nio.charset.StandardCharsets.UTF_8), ArrayList.class);
                List<FrageImport> fragen = importedArray.stream()
                        .map(m -> new FrageImport(
                                text(m.get("thema")),
                                text(m.get("frage")),
                                m.get("antworten") instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of(),
                                text(m.get("correct_answer")),
                                text(m.get("beschreibung")),
                                text(m.get("bild")),
                                Boolean.TRUE.equals(m.get("bild_mit_antwort"))))
                        .toList();
                ImportErgebnis ergebnis = quizItemService.importiere(fragen);
                ergebnis.fehler().forEach(System.err::println);
                Notification.show(ergebnis.angelegt() + " Fragen importiert"
                                + (ergebnis.fehler().isEmpty() ? "" : ", " + ergebnis.fehler().size() + " Fehler"),
                        4000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(ergebnis.fehler().isEmpty()
                                ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_WARNING);
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
        category.setWidthFull();
        add(dialogLayout);
        Responsive.dialog(this, "28em");
    }

    private static String text(Object o) {
        return o == null ? null : o.toString();
    }
}
