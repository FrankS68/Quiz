package de.witchcafe.quiz;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KategorieFavoritRepository extends JpaRepository<KategorieFavorit, Long> {

    List<KategorieFavorit> findByUserIdOrderByPfad(String userId);

    Optional<KategorieFavorit> findByUserIdAndPfad(String userId, String pfad);
}
