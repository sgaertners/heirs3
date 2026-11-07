package de.sgs.heirs.service;


import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.ByteBuffer;
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
    private SecretKey aesMasterKey;

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

        initAesKey();
    }

    // Am Ende der bestehenden init()-Methode aufrufen:
    private void initAesKey() throws Exception {
        byte[] privBytes = masterKeyPair.getPrivate().getEncoded();
        byte[] key256 = MessageDigest.getInstance("SHA-256").digest(privBytes);
        this.aesMasterKey = new SecretKeySpec(key256, "AES");
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

    public String encryptWithMasterKey(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return "";
        try {
            if (this.aesMasterKey == null) initAesKey();
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, this.aesMasterKey, new GCMParameterSpec(128, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes());

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Verschlüsselungsfehler mit Masterkey", e);
        }
    }

    public String decryptWithMasterKey(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) return "";
        try {
            if (this.aesMasterKey == null) initAesKey();
            byte[] cipherMessage = Base64.getUrlDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(cipherMessage);

            byte[] iv = new byte[12];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, this.aesMasterKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new RuntimeException("Entschlüsselungsfehler mit Masterkey", e);
        }
    }


    // In ServerMasterKeyService.java folgende Methoden für byte[] ergänzen:

    /**
     * Verschlüsselt beliebige Rohdaten (z. B. PDF-Bytes) mit AES-256-GCM
     */
    public byte[] encryptBytes(byte[] plainBytes) {
        if (plainBytes == null || plainBytes.length == 0) return new byte[0];
        try {
            if (this.aesMasterKey == null) initAesKey();
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, this.aesMasterKey, new GCMParameterSpec(128, iv));
            byte[] cipherText = cipher.doFinal(plainBytes);

            // Aufbau: [12 Bytes IV] + [Ciphertext + 16 Bytes Auth-Tag]
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return buffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Verschlüsseln der PDF-Bytes", e);
        }
    }

    /**
     * Entschlüsselt beliebige Rohdaten mit AES-256-GCM
     */
    public byte[] decryptBytes(byte[] encryptedBytes) {
        if (encryptedBytes == null || encryptedBytes.length == 0) return new byte[0];
        try {
            if (this.aesMasterKey == null) initAesKey();
            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);

            byte[] iv = new byte[12];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, this.aesMasterKey, new GCMParameterSpec(128, iv));

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Entschlüsseln der PDF-Bytes", e);
        }
    }
}
