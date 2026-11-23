package de.witchcafe.quiz;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {

    // If you don't need a total row count, Slice is better than Page as it only performs a select query.
    // Page performs both a select and a count query.
    Slice<Quiz> findAllBy(Pageable pageable);

    /** Gelaufene Quizze einer Kategorie inkl. Unterkategorien, neueste zuerst. */
    @Query("SELECT q FROM Quiz q WHERE q.category = :pfad OR q.category LIKE CONCAT(:pfad, '/%') ORDER BY q.id DESC")
    List<Quiz> findImTeilbaum(@Param("pfad") String pfad);
}
