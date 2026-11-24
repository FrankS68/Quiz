package de.witchcafe.quiz;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Eine Kategorie (Pfad), die ein Benutzer als Favorit gespeichert hat. */
@Entity
@Table(name = "kategorie_favorit",
        uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "pfad" }))
public class KategorieFavorit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "favorit_id")
    private Long id;

    /** ID des Benutzers aus witch-auth (z.B. "github:123"). */
    @Column(name = "user_id", nullable = false)
    private String userId = "";

    @Column(name = "pfad", nullable = false, length = Kategorie.PFAD_MAX_LENGTH)
    private String pfad = "";

    @Column(name = "angelegt_am")
    private Instant angelegtAm;

    protected KategorieFavorit() { // fuer Hibernate
    }

    public KategorieFavorit(String userId, String pfad) {
        this.userId = userId;
        this.pfad = pfad;
        this.angelegtAm = Instant.now();
    }

    public String getUserId() {
        return userId;
    }

    public String getPfad() {
        return pfad;
    }

    void setPfad(String pfad) {
        this.pfad = pfad;
    }
}
