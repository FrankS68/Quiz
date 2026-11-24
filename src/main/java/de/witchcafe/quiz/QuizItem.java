package de.witchcafe.quiz;


import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "quizitem")
public class QuizItem {

    public static final int DESCRIPTION_MAX_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "quizitem_id")
    private Long id;

    @Column(name = "question", nullable = false)
    private String question = "";

    @Column(name = "answer", nullable = false)
    private String answer = "";

    @Column(name = "category", nullable = false)
    private String category = "";

    @Column(name = "options",length = 4000)
    private ArrayList<String> options = new ArrayList<String>();

    @Column(name = "tags")
    private ArrayList<String> tags =  new ArrayList<String>();

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description = "";

    public static final int BILD_MAX_LENGTH = 1000;

    /**
     * Optionales Bild zur Frage: externe URL (https://...) oder ein hochgeladenes Bild
     * ({@code /bilder/<id>}, siehe {@link Bild}). Leer = kein Bild.
     */
    @Column(name = "bild", length = BILD_MAX_LENGTH)
    private String bild = "";

    /** true: Bild verraet die Loesung und wird erst zusammen mit der Antwort gezeigt. */
    @Column(name = "bild_mit_antwort")
    private Boolean bildMitAntwort = false;

    protected QuizItem() { // To keep Hibernate happy
    }

    public QuizItem(String category, String question, String answer) {
    	this(category,question,answer,"");
    }

    public QuizItem(String category, String question, String answer, String description) {
    	setCategory(category);
    	setQuestion(question);
    	setAnswer(answer);
    	setDescription(description);
    }

    public @Nullable Long getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        QuizItem other = (QuizItem) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Hashcode should never change during the lifetime of an object. Because of
        // this we can't use getId() to calculate the hashcode. Unless you have sets
        // with lots of entities in them, returning the same hashcode should not be a
        // problem.
        return getClass().hashCode();
    }

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<String> getOptions() {
		return options;
	}

	public void setOptions(ArrayList<String> options) {
		this.options = options;
	}

	public ArrayList<String> getTags() {
		return tags;
	}

	public void setTags(ArrayList<String> tags) {
		this.tags = tags;
	}

	/** Bild-URL oder leer (auch bei alten Zeilen, in denen die Spalte noch NULL ist). */
	public String getBild() {
		return bild == null ? "" : bild;
	}

	public void setBild(String bild) {
		this.bild = bild == null ? "" : bild.trim();
	}

	public boolean isBildMitAntwort() {
		return Boolean.TRUE.equals(bildMitAntwort);
	}

	public void setBildMitAntwort(boolean bildMitAntwort) {
		this.bildMitAntwort = bildMitAntwort;
	}

	public boolean hatBild() {
		return !getBild().isEmpty();
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
