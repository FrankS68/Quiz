package de.witchcafe.quiz.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;

    public ApiKeyService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    /**
     * Erzeugt einen neuen API-Key, speichert den Hash und gibt den Klartext-Token zurueck.
     * Der Klartext wird nur einmal angezeigt und ist danach nicht mehr zugreifbar.
     */
    @Transactional
    public String erzeugeApiKey(String name, String rolle) {
        String token = generiereToken();
        repository.save(new ApiKey(name, hash(token), rolle));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<ApiKey> findByToken(String token) {
        return repository.findByTokenHashAndAktivTrue(hash(token));
    }

    @Transactional(readOnly = true)
    public List<ApiKey> alleKeys() {
        return repository.findAll();
    }

    @Transactional
    public void deaktiviere(Long id) {
        repository.findById(id).ifPresent(k -> k.setAktiv(false));
    }

    @Transactional
    public void loesche(Long id) {
        repository.deleteById(id);
    }

    private static String generiereToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "qz_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
