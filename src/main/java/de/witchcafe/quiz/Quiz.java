package de.witchcafe.quiz;


import org.jspecify.annotations.Nullable;

import consulting.segieth.security.UserProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz")
public class Quiz {

    public static final int DESCRIPTION_MAX_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "quiz_id")
    private Long id;

    @Column(name = "category", nullable = false)
    private String category = "";

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id")
    private UserProfile user = null;

    @Column(name = "publish", nullable = false)
    private Boolean publish = true;
    
    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description = "";

    @Column(name = "result")
    private String result = "";

    protected Quiz() { // To keep Hibernate happy
    }

    public Quiz(String category, UserProfile user) {
    	setCategory(category);
    	setUser(user);
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

        Quiz other = (Quiz) obj;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public UserProfile getUser() {
		return user;
	}

	public void setUser(UserProfile user) {
		this.user = user;
	}

	public Boolean getPublish() {
		return publish;
	}

	public void setPublish(Boolean publish) {
		this.publish = publish;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
