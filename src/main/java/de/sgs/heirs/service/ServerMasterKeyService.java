package de.sgs.heirs.service;


import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;

@Service
public class ServerMasterKeyService {

    private static final String PRIVATE_KEY_FILE = "master_private.key";
    private static final String PUBLIC_KEY_FILE = "master_public.key";

    private KeyPair masterKeyPair;

    @PostConstruct
    public void init() throws Exception {
        File privFile = new File(PRIVATE_KEY_FILE);
        File pubFile = new File(PUBLIC_KEY_FILE);

        if (privFile.exists() && pubFile.exists()) {
            // Vorhandene Master-Keys von Festplatte laden
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            byte[] privBytes = Files.readAllBytes(privFile.toPath());
            byte[] pubBytes = Files.readAllBytes(pubFile.toPath());

            PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
            this.masterKeyPair = new KeyPair(pubKey, privKey);
        } else {
            // Erstmalige Generierung des Server-Master-Keypaars
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            this.masterKeyPair = kpg.generateKeyPair();

            // Für Server-Neustarts auf Festplatte sichern
            Files.write(privFile.toPath(), this.masterKeyPair.getPrivate().getEncoded());
            Files.write(pubFile.toPath(), this.masterKeyPair.getPublic().getEncoded());
        }
    }

    /**
     * Erstellt eine digitale Signatur für den Ausweis (username + validUntil)
     */
    public String signBadge(String username, LocalDate validUntil) throws Exception {
        String payload = buildPayload(username, validUntil);
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(masterKeyPair.getPrivate());
        signature.update(payload.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    /**
     * Verifiziert die Signatur mit dem Master Public Key
     */
    public boolean verifyBadge(String username, LocalDate validUntil, String signatureUrlSafeBase64) {
        try {
            String payload = buildPayload(username, validUntil);
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(masterKeyPair.getPublic());
            signature.update(payload.getBytes());

            byte[] sigBytes = Base64.getUrlDecoder().decode(signatureUrlSafeBase64);
            return signature.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildPayload(String username, LocalDate validUntil) {
        return username.trim().toLowerCase() + "|" + validUntil.toString();
    }
}
