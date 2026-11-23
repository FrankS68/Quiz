package de.witchcafe.quiz;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.witchcafe.auth.AppRoles;

@Service
public class QuizItemService {

    private final QuizItemRepository quizItemRepository;
    private final QuizBilling quizBilling;

    QuizItemService(QuizItemRepository quizItemRepository, QuizBilling quizBilling) {
        this.quizItemRepository = quizItemRepository;
        this.quizBilling = quizBilling;
    }

    // Fragen anlegen nur fuer Editoren und Admins (zusaetzlich zum Ausblenden der Knoepfe)
    private static final String CAN_EDIT = "hasAnyRole('" + AppRoles.ADMIN + "','" + QuizRoles.EDITOR + "')";

    @Transactional
    @PreAuthorize(CAN_EDIT)
    public QuizItem createQuizItem(String c,String q,String a,String d) {
        var quizItem = new QuizItem(c,q,a,d);
        quizItemRepository.saveAndFlush(quizItem);
        quizBilling.rewardAfterCommit(QuizBilling.FRAGE_ANGELEGT, 1);
        return quizItem;
    }
    
    @Transactional
    @PreAuthorize(CAN_EDIT)
    public QuizItem createQuizItem(String c,String q,String a,String d,ArrayList<String> o,ArrayList<String> t) {
        var quizItem = new QuizItem(c,q,a,d);
        quizItem.setOptions(o);
        quizItem.setTags(t);
        quizItemRepository.saveAndFlush(quizItem);
        quizBilling.rewardAfterCommit(QuizBilling.FRAGE_ANGELEGT, 1);
        return quizItem;
    }
    
    /**
     * Legt alle Fragen eines Fragenkatalogs an (Upload in der Oberflaeche und REST-API).
     * Fehlerhafte Eintraege werden uebersprungen und im Ergebnis gemeldet.
     * Angemeldete Benutzer (Upload-Dialog) bekommen pro angelegter Frage eine Gutschrift,
     * Importe ueber die REST-API nicht (kein Benutzer).
     */
    @Transactional
    @PreAuthorize(CAN_EDIT)
    public ImportErgebnis importiere(List<FrageImport> fragen) {
        int angelegt = 0;
        List<String> fehler = new ArrayList<>();
        for (int i = 0; i < fragen.size(); i++) {
            FrageImport f = fragen.get(i);
            if (f == null || istLeer(f.thema()) || istLeer(f.frage()) || istLeer(f.correctAnswer())) {
                fehler.add("Eintrag " + (i + 1) + ": thema, frage und correct_answer sind Pflicht");
                continue;
            }
            String beschreibung = f.beschreibung() == null ? "" : f.beschreibung();
            if (beschreibung.length() > QuizItem.DESCRIPTION_MAX_LENGTH) {
                fehler.add("Eintrag " + (i + 1) + ": beschreibung laenger als " + QuizItem.DESCRIPTION_MAX_LENGTH + " Zeichen");
                continue;
            }
            var quizItem = new QuizItem(Kategorie.normalisiere(f.thema()), f.frage().trim(), f.correctAnswer().trim(), beschreibung);
            quizItem.setOptions(f.antworten() == null ? new ArrayList<>() : new ArrayList<>(f.antworten()));
            quizItemRepository.save(quizItem);
            angelegt++;
        }
        quizItemRepository.flush();
        quizBilling.rewardAfterCommit(QuizBilling.FRAGE_IMPORTIERT, angelegt);
        return new ImportErgebnis(angelegt, fehler);
    }

    private static boolean istLeer(String s) {
        return s == null || s.isBlank();
    }

    /** Ergebnis eines Imports: Anzahl angelegter Fragen und Fehler je Eintrag. */
    public record ImportErgebnis(int angelegt, List<String> fehler) {
    }

    @Transactional(readOnly = true)
    public List<QuizItem> findAll() {
        return quizItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<QuizItem> list(Pageable pageable) {
        return quizItemRepository.findAllBy(pageable).toList();
    }
    
    @Transactional(readOnly = true)
    public List<QuizItem> findByCategory(String category) {
        return quizItemRepository.findByCategory(category);
    }
    
    /** Fragen einer Kategorie inkl. aller Unterkategorien (Quiz auf Oberthemen). */
    @Transactional(readOnly = true)
    public List<QuizItem> findImTeilbaum(String pfad) {
        return quizItemRepository.findImTeilbaum(pfad).stream()
                .filter(q -> Kategorie.imTeilbaum(q.getCategory(), pfad))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Object[]> lookupCategories() {
        return quizItemRepository.lookupCategories();
    }
}
