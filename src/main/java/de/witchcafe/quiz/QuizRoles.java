package de.witchcafe.quiz;

/**
 * Quiz-spezifische Rollen, ergaenzend zu de.witchcafe.auth.AppRoles (ADMIN, USER).
 *
 * ADMIN   (aus witch-auth) - alles, inkl. Benutzerverwaltung und Social-Seiten
 * EDITOR  - darf Fragen bearbeiten und Fragenkataloge hochladen
 * USER    (aus witch-auth) - Quiz spielen und Ergebnisse sehen
 */
public final class QuizRoles {

    private QuizRoles() {}

    /** Darf Fragen bearbeiten und Fragenkataloge (JSON) hochladen. */
    public static final String EDITOR = "EDITOR";
}
