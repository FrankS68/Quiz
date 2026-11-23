package de.witchcafe.quiz;

import java.time.Instant;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Ein hochgeladenes Bild zu einer Frage. Liegt in der Datenbank (und damit im Volume-Backup)
 * und wird unter {@code /bilder/{id}} ausgeliefert ({@link BildController}).
 */
@Entity
@Table(name = "bild")
public class Bild {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "bild_id")
    private Long id;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "dateiname", length = 255)
    private String dateiname;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "daten", nullable = false)
    private byte[] daten;

    @Column(name = "angelegt_am", nullable = false)
    private Instant angelegtAm = Instant.now();

    protected Bild() { // fuer Hibernate
    }

    public Bild(String contentType, String dateiname, byte[] daten) {
        this.contentType = contentType;
        this.dateiname = dateiname;
        this.daten = daten;
    }

    public Long getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public String getDateiname() {
        return dateiname;
    }

    public byte[] getDaten() {
        return daten;
    }

    /** Pfad, unter dem das Bild ausgeliefert wird; so steht es auch im Feld {@code bild} der Frage. */
    public String url() {
        return BildController.PFAD + id;
    }
}
