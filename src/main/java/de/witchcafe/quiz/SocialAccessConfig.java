package de.witchcafe.quiz;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.server.auth.AccessCheckResult;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationContext;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;

import de.witchcafe.auth.AppRoles;

/**
 * Die Social-Seiten aus witch-social-core (Feeds, Neuer Beitrag, Beitraege, Konten) sind dort
 * mit {@code @PermitAll} fuer alle Angemeldeten markiert. Im Quiz sollen nur Admins sie sehen.
 *
 * Zusaetzlich zur Annotationspruefung verweigert ein eigener Checker allen Nicht-Admins den
 * Zugriff auf Views aus {@code de.witchcafe.social}. Vaadin lehnt ab, sobald ein Checker
 * ablehnt, und filtert das Menue mit denselben Pruefungen - die Seiten verschwinden also auch
 * aus dem Menue. Ersetzt die Standard-Konfiguration aus vaadin-spring (nur Annotationspruefung);
 * witch-auth (VaadinSecurityConfigurer) schaltet sie ein und setzt die Login-Seite.
 */
@Configuration
public class SocialAccessConfig {

    private static final String SOCIAL_VIEWS_PACKAGE = "de.witchcafe.social";

    @Bean
    NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
        return new NavigationAccessControlConfigurer()
                .withAnnotatedViewAccessChecker()
                .withNavigationAccessChecker(new SocialViewsAdminOnly());
    }

    static class SocialViewsAdminOnly implements NavigationAccessChecker {

        @Override
        public AccessCheckResult check(NavigationContext context) {
            if (!context.getNavigationTarget().getPackageName().startsWith(SOCIAL_VIEWS_PACKAGE)) {
                return context.neutral();
            }
            return context.hasRole(AppRoles.ADMIN)
                    ? context.neutral()
                    : context.deny("Social-Seiten sind im Quiz nur fuer Admins sichtbar");
        }
    }
}
