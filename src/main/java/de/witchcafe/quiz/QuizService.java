package de.witchcafe.quiz;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.witchcafe.auth.AppUserInfo;
import de.witchcafe.auth.domain.AppUser;
import de.witchcafe.auth.domain.AppUserRepository;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final AppUserRepository appUserRepository;

    QuizService(QuizRepository quizRepository, AppUserRepository appUserRepository) {
        this.quizRepository = quizRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public Quiz createQuiz(String c, @Nullable AppUserInfo u) {
        var quiz = new Quiz(c, lookupUser(u));
        quizRepository.saveAndFlush(quiz);
        return quiz;
    }

    @Transactional
    public Quiz createQuiz(String c, @Nullable AppUserInfo u, String r) {
        var quiz = new Quiz(c, lookupUser(u));
        quiz.setResult(r);
        quizRepository.saveAndFlush(quiz);
        return quiz;
    }

    @Transactional(readOnly = true)
    public List<Quiz> list(Pageable pageable) {
        return quizRepository.findAllBy(pageable).toList();
    }

    /**
     * Gelaufene Quizze einer Kategorie inkl. aller Unterkategorien, neueste zuerst.
     * Vorerst sieht jeder Benutzer alle; soll das spaeter eingeschraenkt werden
     * (z.B. nur eigene), dann hier filtern.
     */
    @Transactional(readOnly = true)
    public List<Quiz> resultate(String pfad) {
        return quizRepository.findImTeilbaum(pfad).stream()
                .filter(q -> Kategorie.imTeilbaum(q.getCategory(), pfad))
                .toList();
    }

    /** Anonyme Nutzer (kein Login) speichern ihr Quiz ohne Benutzerbezug. */
    private @Nullable AppUser lookupUser(@Nullable AppUserInfo u) {
        return u == null ? null : appUserRepository.findById(u.getUserId().toString()).orElse(null);
    }
}
