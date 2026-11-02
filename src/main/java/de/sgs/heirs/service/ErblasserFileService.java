package de.sgs.heirs.service;

import de.sgs.heirs.model.FileEntry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class ErblasserFileService {

    private static final String BASE_DATA_DIR = "data";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Prüft anhand von Dateiendung oder Content-Type, ob es sich um Audio oder Video handelt
     */
    public boolean isAudioOrVideo(String filename, String contentType) {
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            if (lowerType.startsWith("audio/") || lowerType.startsWith("video/")) {
                return true;
            }
        }
        String lowerName = filename.toLowerCase();
        return lowerName.matches(".*\\.(mp3|wav|ogg|m4a|flac|aac|wma|mp4|webm|mkv|avi|mov|wmv|flv|m4v)");
    }

    private SecretKey deriveErblasserKey(String erblasserUsername) throws Exception {
        byte[] masterPrivBytes = Files.readAllBytes(Paths.get("master_private.key"));
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(masterPrivBytes, "HmacSHA256"));
        byte[] keyBytes = hmac.doFinal(erblasserUsername.trim().toLowerCase().getBytes());
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Speichert eine Datei.
     * Audio- und Videodateien werden UNVERSCHLÜSSELT gespeichert.
     * Alle anderen Dateien werden mit AES-256-GCM verschlüsselt.
     */
    public synchronized void saveEncryptedFile(String erblasser, String heir, String category, MultipartFile multipartFile) throws Exception {
        String cleanFilename = Paths.get(multipartFile.getOriginalFilename()).getFileName().toString();

        Path erblasserDir = Paths.get(BASE_DATA_DIR, erblasser);
        Path targetDir = erblasserDir.resolve(heir);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(cleanFilename);

        boolean isMedia = isAudioOrVideo(cleanFilename, multipartFile.getContentType());

        if (isMedia) {
            // Audio/Video: Direkte unverschlüsselte Speicherung
            Files.write(targetFile, multipartFile.getBytes());
        } else {
            // Andere Dokumente: AES-256-GCM Verschlüsselung
            SecretKey erblasserKey = deriveErblasserKey(erblasser);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, erblasserKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherBytes = cipher.doFinal(multipartFile.getBytes());

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);

            Files.write(targetFile, buffer.array());
        }

        // CSV-Eintrag aktualisieren
        Path csvFile = erblasserDir.resolve("files.csv");
        String csvLine = String.format("%s;%s;%s%n", heir, cleanFilename, category);
        Files.writeString(csvFile, csvLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    /**
     * Liest eine Datei.
     * Wenn Audio/Video -> direktes Auslesen ohne Entschlüsselung.
     * Wenn anderes Dokument -> Entschlüsselung per AES-GCM.
     */
    public byte[] loadFileContent(String erblasser, String heir, String filename) throws Exception {
        Path filePath = Paths.get(BASE_DATA_DIR, erblasser, heir, filename);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Datei nicht gefunden: " + filename);
        }

        String mimeType = Files.probeContentType(filePath);

        // 1. Audio & Video: Direkt unverschlüsselt zurückgeben!
        if (isAudioOrVideo(filename, mimeType)) {
            return Files.readAllBytes(filePath);
        }

        // 2. Andere Dateien: AES-256-GCM Entschlüsselung
        byte[] fileBytes = Files.readAllBytes(filePath);
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);

        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        SecretKey erblasserKey = deriveErblasserKey(erblasser);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, erblasserKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return cipher.doFinal(cipherText);
    }

    public List<FileEntry> listFilesForErblasser(String erblasser) {
        List<FileEntry> entries = new ArrayList<>();
        Path csvFile = Paths.get(BASE_DATA_DIR, erblasser, "files.csv");
        if (!Files.exists(csvFile)) return entries;

        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String heir = parts[0].trim();
                    String filename = parts[1].trim();
                    String category = parts[2].trim();

                    Path filePath = Paths.get(BASE_DATA_DIR, erblasser, heir, filename);
                    long size = Files.exists(filePath) ? Files.size(filePath) : 0;
                    String mimeType = Files.probeContentType(filePath);
                    if (mimeType == null) mimeType = "application/octet-stream";

                    entries.add(new FileEntry(erblasser, heir, filename, category, mimeType, size));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;
    }

    /**
     * Entschlüsselt die gespeicherte Datei im Arbeitsspeicher
     */
    public byte[] decryptFile(String erblasser, String heir, String filename) throws Exception {
        Path filePath = Paths.get(BASE_DATA_DIR, erblasser, heir, filename);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Datei existiert nicht: " + filename);
        }

        byte[] fileBytes = Files.readAllBytes(filePath);
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);

        // 1. Die ersten 12 Bytes als IV extrahieren
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        // 2. Restliche Bytes als Ciphertext + Auth-Tag
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        // 3. Mit dem Erblasser-Schlüssel entschlüsseln
        SecretKey erblasserKey = deriveErblasserKey(erblasser);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, erblasserKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return cipher.doFinal(cipherText);
    }

    /**
     * Sucht alle Dokumente, die für einen bestimmten Erben bestimmt sind
     */
    public List<FileEntry> listFilesForHeir(String heirUsername, String erblasserUsername) {
        List<FileEntry> entries = new ArrayList<>();
        if (erblasserUsername == null || erblasserUsername.isBlank()) return entries;

        Path csvFile = Paths.get(BASE_DATA_DIR, erblasserUsername, "files.csv");
        if (!Files.exists(csvFile)) return entries;

        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String targetHeir = parts[0].trim();
                    String filename = parts[1].trim();
                    String category = parts[2].trim();

                    if (targetHeir.equalsIgnoreCase(heirUsername.trim())) {
                        Path filePath = Paths.get(BASE_DATA_DIR, erblasserUsername, targetHeir, filename);
                        long size = Files.exists(filePath) ? Files.size(filePath) : 0;
                        String mimeType = Files.probeContentType(filePath);
                        if (mimeType == null) mimeType = "application/octet-stream";

                        entries.add(new FileEntry(erblasserUsername, targetHeir, filename, category, mimeType, size));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return entries;
    }

}
