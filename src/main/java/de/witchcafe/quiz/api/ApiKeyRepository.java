package de.witchcafe.quiz.api;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByTokenHashAndAktivTrue(String tokenHash);
}
