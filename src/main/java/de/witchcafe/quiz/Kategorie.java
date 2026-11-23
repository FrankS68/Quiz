package de.witchcafe.quiz;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Zusatzdaten zu einer Kategorie. Die Kategorie selbst ist ein Pfad im Thema der Fragen,
 * Ebenen getrennt durch '/', z.B. "Drohnen/EU A1-A3/Luftrecht" (Oberthema/Zertifikat/Abschnitt).
 * Ein Eintrag hier ist nur noetig, wenn es Zusatzdaten gibt (Schlagworte) - auch fuer
 * Oberthemen ohne eigene Fragen.
 */
@Entity
@Table(name = "kategorie")
public class Kategorie {

    public static final int PFAD_MAX_LENGTH = 500;
    public static final int SCHLAGWORTE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "kategorie_id")
    private Long id;

    @Column(name = "pfad", nullable = false, unique = true, length = PFAD_MAX_LENGTH)
    private String pfad = "";

    /** Suchbare Schlagworte, durch Komma getrennt. */
    @Column(name = "schlagworte", length = SCHLAGWORTE_MAX_LENGTH)
    private String schlagworte = "";

    protected Kategorie() { // fuer Hibernate
    }

    public Kategorie(String pfad) {
        this.pfad = normalisiere(pfad);
    }

    public Long getId() {
        return id;
    }

    public String getPfad() {
        return pfad;
    }

    void setPfad(String pfad) {
        this.pfad = normalisiere(pfad);
    }

    public String getSchlagworte() {
        return schlagworte == null ? "" : schlagworte;
    }

    public void setSchlagworte(String schlagworte) {
        this.schlagworte = normalisiereSchlagworte(schlagworte);
    }

    /** "Drohnen / EU A1-A3//Luftrecht " -> "Drohnen/EU A1-A3/Luftrecht" */
    public static String normalisiere(String pfad) {
        if (pfad == null) {
            return "";
        }
        return Arrays.stream(pfad.split("/"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
    }

    /** Komma-getrennt, ohne leere Eintraege und Dubletten (Gross-/Kleinschreibung egal). */
    public static String normalisiereSchlagworte(String text) {
        if (text == null) {
            return "";
        }
        var gesehen = new java.util.LinkedHashMap<String, String>();
        Arrays.stream(text.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> gesehen.putIfAbsent(s.toLowerCase(), s));
        return String.join(", ", gesehen.values());
    }

    /** Liegt {@code pfad} im Teilbaum von {@code wurzel} (oder ist es die Wurzel selbst)? */
    public static boolean imTeilbaum(String pfad, String wurzel) {
        return pfad.equals(wurzel) || pfad.startsWith(wurzel + "/");
    }
}
