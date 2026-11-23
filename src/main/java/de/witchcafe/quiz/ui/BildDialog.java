package de.witchcafe.quiz.ui;

import java.io.IOException;
import java.io.InputStream;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.ui.Responsive;

/**
 * Bild einer Frage setzen (Editoren/Admins): Datei hochladen oder externe URL eintragen,
 * mit Vorschau; leere URL entfernt das Bild.
 */
public class BildDialog extends Dialog {

    private final QuizItemService quizItemService;
    private final TextField url = new TextField("Bild-URL");
    private final Div vorschau = new Div();

    public BildDialog(QuizItem item, QuizItemService quizItemService, Runnable nachSpeichern) {
        this.quizItemService = quizItemService;
        setHeaderTitle("Bild zur Frage");

        Span frage = new Span(item.getQuestion());
        frage.getStyle().setFontWeight("600");

        url.setValue(item.getBild());
        url.setWidthFull();
        url.setClearButtonVisible(true);
        url.setPlaceholder("https://… oder Datei hochladen");
        url.setHelperText("Externe https-URL oder hochgeladenes Bild (/bilder/…). Leer = kein Bild.");
        url.addValueChangeListener(e -> zeigeVorschau());

        MemoryBuffer puffer = new MemoryBuffer();
        Upload upload = new Upload(puffer);
        upload.setAcceptedFileTypes(QuizItemService.BILD_TYPEN.toArray(String[]::new));
        upload.setMaxFileSize((int) QuizItemService.BILD_MAX_BYTES);
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Bild hierher ziehen (PNG, JPEG, WebP, GIF, max. 2 MB)"));
        upload.addSucceededListener(e -> {
            try (InputStream in = puffer.getInputStream()) {
                url.setValue(quizItemService.speichereBild(e.getMIMEType(), e.getFileName(), in.readAllBytes()));
                upload.clearFileList();
            } catch (IOException | IllegalArgumentException ex) {
                fehler(ex.getMessage());
            }
        });
        upload.addFileRejectedListener(e -> fehler(e.getErrorMessage()));

        vorschau.setWidthFull();
        zeigeVorschau();

        VerticalLayout layout = new VerticalLayout(frage, upload, url, vorschau);
        layout.setPadding(false);
        add(layout);

        Button speichern = new Button("Speichern", e -> {
            try {
                quizItemService.setzeBild(item.getId(), url.getValue());
                Notification.show(url.getValue().isBlank() ? "Bild entfernt" : "Bild gespeichert",
                        3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                nachSpeichern.run();
                close();
            } catch (IllegalArgumentException ex) {
                url.setInvalid(true);
                url.setErrorMessage(ex.getMessage());
            }
        });
        speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        Responsive.dialog(this, "32em");
    }

    private void zeigeVorschau() {
        vorschau.removeAll();
        url.setInvalid(false);
        String u = url.getValue().trim();
        if (u.isEmpty()) {
            return;
        }
        String fehler = QuizItemService.pruefeBildUrl(u);
        if (fehler != null) {
            url.setInvalid(true);
            url.setErrorMessage(fehler);
            return;
        }
        vorschau.add(QuizLaufDialog.bildKomponente(u));
    }

    private static void fehler(String text) {
        Notification.show(text, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
