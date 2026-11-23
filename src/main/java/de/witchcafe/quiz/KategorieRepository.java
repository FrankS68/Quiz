package de.witchcafe.quiz;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KategorieRepository extends JpaRepository<Kategorie, Long> {

    Optional<Kategorie> findByPfad(String pfad);
}
