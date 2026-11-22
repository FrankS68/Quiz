package de.witchcafe.quiz;

import java.util.Optional;
import java.util.OptionalLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import de.witchcafe.billing.client.BillingClient;
import de.witchcafe.billing.client.BillingException;
import de.witchcafe.billing.client.IdempotencyKeys;
import de.witchcafe.billing.client.InsufficientCreditException;
import de.witchcafe.billing.client.api.BookingResponse;

/**
 * Anbindung des Quiz an witch-billing (gemeinsames Guthabenkonto aller Apps).
 * <p>
 * Features in der Preisliste des Billing-Dienstes (App "quiz"):
 * <ul>
 * <li>{@value #SPIEL} (Verbrauch): eine Quiz-Runde, gebucht beim Start</li>
 * <li>{@value #FRAGE_ANGELEGT} (Beitrag): eine Frage angelegt</li>
 * <li>{@value #FRAGE_IMPORTIERT} (Beitrag): pro Frage aus einem hochgeladenen Fragenkatalog</li>
 * </ul>
 * Gebucht wird fuer den angemeldeten Benutzer (spielen geht nur mit Login). Importe ueber die
 * REST-API laufen am Billing vorbei, dort gibt es keinen Benutzer.
 */
@Service
public class QuizBilling {

    public static final String SPIEL            = "spiel";
    public static final String FRAGE_ANGELEGT   = "frage-angelegt";
    public static final String FRAGE_IMPORTIERT = "frage-importiert";

    private static final Logger log = LoggerFactory.getLogger(QuizBilling.class);

    private final BillingClient billing;

    QuizBilling(BillingClient billing) {
        this.billing = billing;
    }

    /**
     * Bucht eine Quiz-Runde fuer den angemeldeten Benutzer.
     *
     * @return die Buchung, oder {@link BookingResponse#NOT_BOOKED} (kein Benutzer, Billing aus,
     *         Dienst nicht erreichbar, Preisliste unvollstaendig)
     * @throws InsufficientCreditException Guthaben reicht nicht; die Runde nicht starten
     */
    public BookingResponse chargeRound() {
        if (billing.currentUserId().isEmpty()) {
            return BookingResponse.NOT_BOOKED;
        }
        try {
            return billing.charge(SPIEL, 1);
        } catch (InsufficientCreditException e) {
            throw e;
        } catch (BillingException e) {
            // Fehler in der Preisliste soll nicht das ganze Quiz lahmlegen
            log.error("Billing-Fehler beim Quiz-Start ({}), Runde wird ohne Buchung erlaubt: {}",
                    e.getCode(), e.getMessage());
            return BookingResponse.NOT_BOOKED;
        }
    }

    /**
     * Schreibt dem angemeldeten Benutzer einen Beitrag gut, laeuft gerade eine Transaktion,
     * erst nach deren Commit (damit nur wirklich gespeicherte Fragen belohnt werden).
     * Ohne angemeldeten Benutzer passiert nichts; Fehler werden nur geloggt.
     */
    public void rewardAfterCommit(String feature, int quantity) {
        Optional<String> userId = billing.currentUserId();
        if (userId.isEmpty() || quantity <= 0) {
            return;
        }
        String key = IdempotencyKeys.next();
        Runnable reward = () -> {
            try {
                billing.reward(userId.get(), feature, quantity, key);
            } catch (BillingException e) {
                log.warn("Gutschrift {} x{} fuer {} fehlgeschlagen ({}): {}",
                        feature, quantity, userId.get(), e.getCode(), e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    reward.run();
                }
            });
        } else {
            reward.run();
        }
    }

    /** Kontostand des angemeldeten Benutzers; leer, wenn anonym oder Billing nicht verfuegbar. */
    public OptionalLong balance() {
        return billing.balance();
    }
}
