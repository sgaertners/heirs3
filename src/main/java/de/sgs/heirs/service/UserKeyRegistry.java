package de.sgs.heirs.service;


import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserKeyRegistry {

    private final Map<String, String> keyToUserMap = new ConcurrentHashMap<>();

    public void registerUser(String username, String publicKeyBase64) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username darf nicht leer sein!");
        }
        if (publicKeyBase64 == null || publicKeyBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("Public Key darf nicht leer sein!");
        }
        keyToUserMap.put(publicKeyBase64, username.trim());
    }

    public Optional<String> findUsernameByPublicKey(String publicKeyBase64) {
        return Optional.ofNullable(keyToUserMap.get(publicKeyBase64));
    }
}