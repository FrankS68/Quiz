package de.witchcafe.quiz;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Macht aus einem Antworttext einen Lueckentext: die signifikanten Stichworte (Zahlen,
 * Abkuerzungen, laengere Hauptwoerter) werden zu Luecken, die eingetippt werden muessen.
 * Woerter, die schon in der Frage stehen, verraten nichts und werden nur notfalls genommen.
 *
 * @param teile der Antworttext in Reihenfolge, abwechselnd fester Text und Luecken
 */
public record Lueckentext(List<Teil> teile) {

    /** Ein Stueck des Antworttextes; bei {@code luecke == true} ist {@code text} die Loesung. */
    public record Teil(String text, boolean luecke) {
    }

    /** Ein Wort inkl. Zahlen wie "3,7", "01.01.2024", "2,4" oder Bindestrich-Woerter wie "LiPo-Akkus". */
    private static final Pattern WORT = Pattern.compile("[\\p{L}\\p{N}]+(?:[.,:\\-][\\p{L}\\p{N}]+)*");

    private static final Set<String> STOPPWOERTER = Set.of(
            "der", "die", "das", "den", "dem", "des", "ein", "eine", "einer", "eines", "einem", "einen",
            "und", "oder", "aber", "auch", "als", "wie", "wenn", "dass", "weil", "denn", "sondern", "sowie",
            "ist", "sind", "war", "wird", "werden", "wurde", "kann", "können", "muss", "müssen", "darf", "dürfen",
            "soll", "sollte", "sollten", "hat", "haben", "sein", "nicht", "kein", "keine", "keinen", "nur", "noch",
            "mit", "ohne", "von", "vom", "zum", "zur", "bei", "beim", "für", "über", "unter", "nach", "vor", "aus",
            "auf", "an", "am", "im", "in", "ins", "zu", "bis", "durch", "gegen", "um", "pro", "je",
            "es", "er", "sie", "man", "sich", "jemand", "dieser", "diese", "dieses", "jede", "jeder", "jedes",
            "alle", "allen", "aller", "immer", "sehr", "mehr", "weniger", "mindestens", "maximal", "höchstens",
            "ja", "nein", "bzw", "z.b", "zb", "etc", "usw", "dabei", "dafür", "dadurch", "damit", "deren", "dessen",
            "welche", "welcher", "welches", "was", "wer", "wo", "da", "so", "dann", "hier", "dort", "ab",
            "grundsätzlich", "regel", "typischerweise", "möglich", "gilt", "gelten", "liegt");

    /** Ab dieser Punktzahl wird ein Wort auch als zweite/dritte Luecke genommen. */
    private static final int MIN_PUNKTE = 6;

    /** Lueckentext aus der Antwort; leer, wenn die Antwort kein brauchbares Stichwort enthaelt. */
    public static Optional<Lueckentext> aus(String frage, String antwort) {
        if (antwort == null || antwort.isBlank()) {
            return Optional.empty();
        }
        Set<String> frageWoerter = new HashSet<>();
        Matcher fm = WORT.matcher(frage == null ? "" : frage);
        while (fm.find()) {
            frageWoerter.add(normalisiere(fm.group()));
        }

        record Kandidat(int start, int ende, String wort, int punkte) {
        }
        List<Kandidat> alle = new ArrayList<>();
        int anzahlWoerter = 0;
        Matcher m = WORT.matcher(antwort);
        while (m.find()) {
            anzahlWoerter++;
            String davor = antwort.substring(0, m.start()).stripTrailing();
            boolean satzanfang = davor.isEmpty() || davor.endsWith(".") || davor.endsWith(":")
                    || davor.endsWith("!") || davor.endsWith("?");
            int punkte = punkte(m.group(), satzanfang, frageWoerter);
            if (punkte > 0) {
                alle.add(new Kandidat(m.start(), m.end(), m.group(), punkte));
            }
        }
        if (alle.isEmpty()) {
            return Optional.empty();
        }

        int maxLuecken = anzahlWoerter <= 3 ? 1 : anzahlWoerter <= 10 ? 2 : 3;
        List<Kandidat> sortiert = new ArrayList<>(alle);
        sortiert.sort(Comparator.comparingInt(Kandidat::punkte).reversed().thenComparingInt(Kandidat::start));
        List<Kandidat> gewaehlt = new ArrayList<>();
        for (Kandidat k : sortiert) {
            if (gewaehlt.size() >= maxLuecken) {
                break;
            }
            // mindestens eine Luecke, weitere nur, wenn sie wirklich signifikant sind
            if (!gewaehlt.isEmpty() && k.punkte() < MIN_PUNKTE) {
                break;
            }
            // dasselbe Wort nicht zweimal abfragen
            if (gewaehlt.stream().noneMatch(g -> normalisiere(g.wort()).equals(normalisiere(k.wort())))) {
                gewaehlt.add(k);
            }
        }
        if (gewaehlt.get(0).punkte() < 2) {
            return Optional.empty();
        }
        gewaehlt.sort(Comparator.comparingInt(Kandidat::start));

        List<Teil> teile = new ArrayList<>();
        int pos = 0;
        for (Kandidat k : gewaehlt) {
            if (k.start() > pos) {
                teile.add(new Teil(antwort.substring(pos, k.start()), false));
            }
            teile.add(new Teil(k.wort(), true));
            pos = k.ende();
        }
        if (pos < antwort.length()) {
            teile.add(new Teil(antwort.substring(pos), false));
        }
        return Optional.of(new Lueckentext(List.copyOf(teile)));
    }

    /** Wie signifikant ein Wort als Stichwort ist; 0 = nie als Luecke. */
    static int punkte(String wort, boolean satzanfang, Set<String> frageWoerter) {
        String klein = wort.toLowerCase(Locale.GERMAN);
        if (STOPPWOERTER.contains(klein)) {
            return 0;
        }
        boolean hatZiffer = wort.chars().anyMatch(Character::isDigit);
        long grossbuchstaben = wort.chars().filter(Character::isUpperCase).count();
        boolean abkuerzung = !hatZiffer && grossbuchstaben >= 2 && grossbuchstaben * 2 >= wort.replaceAll("\\P{L}", "").length();

        int punkte;
        if (hatZiffer) {
            punkte = 10;
        } else if (abkuerzung) {
            punkte = 9;
        } else if (wort.length() < 4) {
            return 0;
        } else if (Character.isUpperCase(wort.charAt(0)) && !satzanfang) {
            // Hauptwort: je laenger, desto eher ein Fachbegriff (Kapazitaet, Lagestabilisierung)
            punkte = Math.min(8, 2 + wort.length() / 2);
        } else if (Character.isUpperCase(wort.charAt(0)) && wort.length() >= 5) {
            // grossgeschrieben am Satzanfang: nur als einzige Luecke ("Knoten (kt).")
            punkte = 3;
        } else if (wort.length() >= 10) {
            // lange Adjektive/Verben: elektromagnetisch, unverzueglich, versicherungspflichtig
            punkte = Math.min(7, 4 + (wort.length() - 10) / 4);
        } else {
            punkte = 1;
        }
        // steht schon in der Frage -> verraet nichts; als Notloesung trotzdem moeglich
        if (frageWoerter.contains(normalisiere(wort))) {
            punkte = 1;
        }
        return punkte;
    }

    /**
     * Ob die Eingabe zur Loesung passt: Gross-/Kleinschreibung, Umlaute (ae/ä), Satzzeichen und
     * Leerzeichen egal; bei laengeren Woertern ohne Ziffern ist ein Tippfehler erlaubt
     * (ab 10 Zeichen zwei). Zahlen muessen stimmen ("3,7" = "3.7").
     */
    public static boolean passt(String eingabe, String loesung) {
        String e = normalisiere(eingabe == null ? "" : eingabe);
        String l = normalisiere(loesung);
        if (e.isEmpty()) {
            return false;
        }
        if (e.equals(l)) {
            return true;
        }
        if (l.chars().anyMatch(Character::isDigit)) {
            return false;
        }
        int erlaubt = l.length() >= 10 ? 2 : l.length() >= 6 ? 1 : 0;
        return erlaubt > 0 && tippfehler(e, l) <= erlaubt;
    }

    static String normalisiere(String s) {
        String t = s.toLowerCase(Locale.GERMAN)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** Editierdistanz, bei der auch ein Buchstabendreher ("ie"/"ei") nur ein Fehler ist. */
    static int tippfehler(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int kosten = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + kosten);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    /** Anzahl der Luecken. */
    public long anzahlLuecken() {
        return teile.stream().filter(Teil::luecke).count();
    }
}
