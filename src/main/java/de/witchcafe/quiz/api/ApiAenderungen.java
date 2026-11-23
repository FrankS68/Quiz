package de.witchcafe.quiz.api;

import java.util.List;

/**
 * Aenderungsprotokoll der REST-API (analog knownet).
 * <p>
 * <b>Bei jeder Aenderung an der API hier oben einen Eintrag ergaenzen</b> (neueste zuerst).
 * Die Version des obersten Eintrags ist die aktuelle API-Version; sie steht in
 * {@code GET /api/docs} und in der OpenAPI-Beschreibung.
 * <p>
 * Versionierung: MAJOR bei inkompatiblen Aenderungen ({@code inkompatibel = true}: Felder/Endpunkte
 * entfernt oder umbenannt, Pflichtfelder neu), MINOR bei Erweiterungen, die bestehende Aufrufe nicht brechen.
 */
public final class ApiAenderungen {

    private ApiAenderungen() {}

    public record Eintrag(String version, String datum, boolean inkompatibel, List<String> aenderungen) {}

    public static final List<Eintrag> PROTOKOLL = List.of(
            new Eintrag("1.3.0", "2026-11-23", false, List.of(
                    "Fragen haben ein optionales Feld 'bild': externe http(s)-URL oder ein in der Oberflaeche "
                            + "hochgeladenes Bild ('/bilder/<id>'). Import (POST /api/fragen) nimmt es an, "
                            + "GET /api/fragen liefert es mit (leer = kein Bild). Andere Werte werden als Fehler gemeldet.")),
            new Eintrag("1.2.0", "2026-11-23", false, List.of(
                    "Kategorien sind hierarchisch: Ebenen im 'thema' mit '/' trennen, z.B. "
                            + "\"Drohnen/EU A1-A3/Luftrecht\" (Oberthema/Zertifikat/Abschnitt). "
                            + "Leerzeichen um '/' werden beim Import entfernt.",
                    "GET /api/kategorien: liefert jetzt jeden Knoten des Baums, auch Oberthemen ohne eigene Fragen; "
                            + "neue Felder 'oberkategorie', 'anzahlGesamt' (inkl. Unterbereiche) und 'schlagworte'. "
                            + "'name' ist der volle Pfad, 'anzahl' wie bisher die Fragen genau dieser Kategorie.",
                    "GET /api/fragen?kategorie=...: liefert die Fragen der Kategorie inkl. aller Unterbereiche.")),
            new Eintrag("1.1.0", "2026-11-22", false, List.of(
                    "GET /api/docs beschreibt jetzt, was die API kann: 'beschreibung', 'endpunkte' "
                            + "(Methode, Pfad, noetige Rolle, Zweck), 'rollen', 'uploadDatei', 'frageFormat' und 'fehlercodes'.",
                    "Neu: GET /api/docs/anleitung - Anleitung mit Beispielen als Markdown.",
                    "Neu: GET /api/docs/aenderungen - dieses Aenderungsprotokoll.")),
            new Eintrag("1.0.0", "2026-11-21", false, List.of(
                    "Ausgangsstand: GET /api/kategorien, GET /api/fragen, POST /api/fragen (JSON oder Datei), "
                            + "/api/docs und /api/docs/openapi.json.")));

    public static String aktuelleVersion() {
        return PROTOKOLL.get(0).version();
    }
}
