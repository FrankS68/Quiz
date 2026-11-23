package de.witchcafe.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.witchcafe.auth.AppRoles;

/**
 * Kategorien als Baum: Pfade aus den Themen der Fragen ('/' trennt die Ebenen, z.B.
 * "Drohnen/EU A1-A3/Luftrecht"), ergaenzt um Schlagworte (Tabelle kategorie) und
 * Favoriten je Benutzer (Tabelle kategorie_favorit).
 */
@Service
public class KategorieService {

    private static final String CAN_EDIT = "hasAnyRole('" + AppRoles.ADMIN + "','" + QuizRoles.EDITOR + "')";

    /** Ein Knoten im Baum mit eigenen Fragen, Fragen im ganzen Teilbaum und Schlagworten. */
    public record Info(String pfad, long eigeneFragen, long fragenGesamt, String schlagworte) {

        /** Letzte Ebene des Pfads, z.B. "Luftrecht". */
        public String name() {
            int i = pfad.lastIndexOf('/');
            return i < 0 ? pfad : pfad.substring(i + 1);
        }

        /** Pfad der Elternebene oder null auf oberster Ebene. */
        public String eltern() {
            int i = pfad.lastIndexOf('/');
            return i < 0 ? null : pfad.substring(0, i);
        }
    }

    private final QuizItemRepository quizItemRepository;
    private final QuizRepository quizRepository;
    private final KategorieRepository kategorieRepository;
    private final KategorieFavoritRepository favoritRepository;

    KategorieService(QuizItemRepository quizItemRepository, QuizRepository quizRepository,
                     KategorieRepository kategorieRepository, KategorieFavoritRepository favoritRepository) {
        this.quizItemRepository = quizItemRepository;
        this.quizRepository = quizRepository;
        this.kategorieRepository = kategorieRepository;
        this.favoritRepository = favoritRepository;
    }

    /**
     * Alle Knoten des Baums, sortiert nach Pfad (Eltern vor Kindern). Enthaelt auch
     * Oberthemen, die nur als Pfadanfang vorkommen oder nur Schlagworte haben.
     */
    @Transactional(readOnly = true)
    public List<Info> alle() {
        Map<String, Long> eigene = new TreeMap<>();
        for (Object[] r : quizItemRepository.lookupCategories()) {
            String pfad = Kategorie.normalisiere(r[0].toString());
            if (!pfad.isEmpty()) {
                eigene.merge(pfad, ((Number) r[1]).longValue(), Long::sum);
            }
        }
        Map<String, String> schlagworte = new TreeMap<>();
        kategorieRepository.findAll().forEach(k -> schlagworte.put(k.getPfad(), k.getSchlagworte()));

        // Alle Pfade samt Oberthemen sammeln
        TreeMap<String, Long> gesamt = new TreeMap<>();
        java.util.Set<String> pfade = new java.util.TreeSet<>();
        pfade.addAll(eigene.keySet());
        pfade.addAll(schlagworte.keySet());
        for (String pfad : pfade) {
            String[] teile = pfad.split("/");
            StringBuilder p = new StringBuilder();
            for (String teil : teile) {
                if (p.length() > 0) {
                    p.append('/');
                }
                p.append(teil);
                gesamt.merge(p.toString(), eigene.getOrDefault(pfad, 0L), Long::sum);
            }
        }
        List<Info> liste = new ArrayList<>();
        gesamt.forEach((pfad, summe) -> liste.add(
                new Info(pfad, eigene.getOrDefault(pfad, 0L), summe, schlagworte.getOrDefault(pfad, ""))));
        return liste;
    }

    @Transactional
    @PreAuthorize(CAN_EDIT)
    public void speichereSchlagworte(String pfad, String schlagworte) {
        String p = Kategorie.normalisiere(pfad);
        Kategorie k = kategorieRepository.findByPfad(p).orElseGet(() -> new Kategorie(p));
        k.setSchlagworte(schlagworte);
        kategorieRepository.save(k);
    }

    /**
     * Benennt eine Kategorie um bzw. verschiebt sie samt allen Unterkategorien:
     * Fragen, gelaufene Quizze, Schlagworte und Favoriten ziehen mit.
     * Existiert das Ziel schon, werden die Kategorien zusammengelegt.
     *
     * @return Anzahl verschobener Fragen
     */
    @Transactional
    @PreAuthorize(CAN_EDIT)
    public int verschiebe(String altPfad, String neuPfad) {
        String alt = Kategorie.normalisiere(altPfad);
        String neu = Kategorie.normalisiere(neuPfad);
        if (alt.isEmpty() || neu.isEmpty()) {
            throw new IllegalArgumentException("Pfad darf nicht leer sein");
        }
        if (alt.equals(neu)) {
            return 0;
        }
        if (Kategorie.imTeilbaum(neu, alt)) {
            throw new IllegalArgumentException("Eine Kategorie kann nicht in ihre eigene Unterkategorie verschoben werden");
        }
        int fragen = 0;
        for (QuizItem q : quizItemRepository.findImTeilbaum(alt)) {
            String c = Kategorie.normalisiere(q.getCategory());
            if (Kategorie.imTeilbaum(c, alt)) {
                q.setCategory(neu + c.substring(alt.length()));
                fragen++;
            }
        }
        for (Quiz q : quizRepository.findImTeilbaum(alt)) {
            if (Kategorie.imTeilbaum(q.getCategory(), alt)) {
                q.setCategory(neu + q.getCategory().substring(alt.length()));
            }
        }
        for (Kategorie k : kategorieRepository.findAll()) {
            if (!Kategorie.imTeilbaum(k.getPfad(), alt)) {
                continue;
            }
            String ziel = neu + k.getPfad().substring(alt.length());
            Optional<Kategorie> vorhanden = kategorieRepository.findByPfad(ziel);
            if (vorhanden.isPresent()) {
                vorhanden.get().setSchlagworte(vorhanden.get().getSchlagworte() + ", " + k.getSchlagworte());
                kategorieRepository.delete(k);
            } else {
                k.setPfad(ziel);
            }
        }
        kategorieRepository.flush();
        for (KategorieFavorit f : favoritRepository.findAll()) {
            if (!Kategorie.imTeilbaum(f.getPfad(), alt)) {
                continue;
            }
            String ziel = neu + f.getPfad().substring(alt.length());
            if (favoritRepository.findByUserIdAndPfad(f.getUserId(), ziel).isPresent()) {
                favoritRepository.delete(f);
            } else {
                f.setPfad(ziel);
            }
        }
        return fragen;
    }

    @Transactional(readOnly = true)
    public List<String> favoriten(String userId) {
        return favoritRepository.findByUserIdOrderByPfad(userId).stream().map(KategorieFavorit::getPfad).toList();
    }

    /** Schaltet den Favoriten um; liefert true, wenn er danach gesetzt ist. */
    @Transactional
    public boolean wechsleFavorit(String userId, String pfad) {
        Optional<KategorieFavorit> f = favoritRepository.findByUserIdAndPfad(userId, pfad);
        if (f.isPresent()) {
            favoritRepository.delete(f.get());
            return false;
        }
        favoritRepository.save(new KategorieFavorit(userId, pfad));
        return true;
    }
}
