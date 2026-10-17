package de.sgs.heirs.service;


import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class Ed25519Service {

    // 1. Generiert ein neues Ed25519 Schlüsselpaar
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        return kpg.generateKeyPair();
    }

    // 2. Signiert Daten mit dem Private Key
    public String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    // 3. Verifiziert die Signatur anhand des Public Keys
    public boolean verify(String data, String signatureBase64, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());
            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    // Wandelt einen Base64-String zurück in ein PublicKey-Objekt
    public PublicKey parsePublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    // Hilfsmethode: PublicKey als Base64
    public String toBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}