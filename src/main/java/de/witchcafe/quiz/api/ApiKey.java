package de.witchcafe.quiz.api;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * API-Key fuer maschinellen Zugriff (z.B. durch KI-Agenten), analog knownet.
 * Der Token wird nur gehasht gespeichert (SHA-256).
 */
@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private String rolle;

    @Column(name = "erstellt_am", nullable = false)
    private LocalDateTime erstelltAm = LocalDateTime.now();

    private boolean aktiv = true;

    protected ApiKey() { // fuer Hibernate
    }

    public ApiKey(String name, String tokenHash, String rolle) {
        this.name = name;
        this.tokenHash = tokenHash;
        this.rolle = rolle;
    }

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getTokenHash()         { return tokenHash; }
    public String getRolle()             { return rolle; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
    public boolean isAktiv()             { return aktiv; }
    public void setAktiv(boolean aktiv)  { this.aktiv = aktiv; }
}
