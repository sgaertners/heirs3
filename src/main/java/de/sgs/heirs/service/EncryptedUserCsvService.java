package de.sgs.heirs.service;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.sgs.heirs.model.EncryptedUserCsvRecord;
import de.sgs.heirs.model.UserAccount;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class EncryptedUserCsvService {

    private static final String CSV_FILE_PATH = "users_secure.csv";
    private static final int GCM_IV_LENGTH = 12;   // 96-Bit IV für AES-GCM
    private static final int GCM_TAG_LENGTH = 128; // 128-Bit Auth-Tag

    private final ServerMasterKeyService masterKeyService;
    private final CsvMapper csvMapper;
    private final CsvSchema writeSchema;
    private final CsvSchema readSchema;

    private SecretKey aesKey;

    public EncryptedUserCsvService(ServerMasterKeyService masterKeyService) {
        this.masterKeyService = masterKeyService;

        this.csvMapper = new CsvMapper();
        // Verhindert Fehler, wenn unbekannte Spalten in alten CSV-Dateien existieren
        this.csvMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Schema für das Schreiben (definiert die Reihenfolge der Spalten)
        this.writeSchema = csvMapper.schemaFor(EncryptedUserCsvRecord.class)
                .withHeader()
                .withColumnSeparator(';');

        // Schema für das Lesen (liest flexibel anhand der Spaltennamen im Header!)
        this.readSchema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnSeparator(';');
    }

    @PostConstruct
    public void init() throws Exception {
        // Leite aus dem privaten Master-Key einen symmetrischen 256-Bit AES-Schlüssel ab
        File privKeyFile = new File("master_private.key");
        if (privKeyFile.exists()) {
            byte[] masterPrivBytes = Files.readAllBytes(privKeyFile.toPath());
            byte[] key256 = MessageDigest.getInstance("SHA-256").digest(masterPrivBytes);
            this.aesKey = new SecretKeySpec(key256, "AES");
        }
    }

    // =========================================================================
    // 1. STREAMEND SPEICHERN (APPEND)
    // =========================================================================
    public synchronized void saveUser(UserAccount user) throws IOException {
        Path path = Paths.get(CSV_FILE_PATH);
        boolean fileExists = Files.exists(path) && Files.size(path) > 0;

        // Header nur schreiben, wenn die Datei neu erstellt wird
        CsvSchema effectiveWriteSchema = fileExists ? writeSchema.withoutHeader() : writeSchema.withHeader();

        EncryptedUserCsvRecord record = encryptUser(user);

        try (OutputStream os = Files.newOutputStream(path,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
             SequenceWriter writer = csvMapper.writer(effectiveWriteSchema).writeValues(os)) {
            writer.write(record);
        }
    }

    // =========================================================================
    // 2. USER SUCHEN (STREAMEND, STOPPT BEIM ERSTEN TREFFER)
    // =========================================================================
    public Optional<UserAccount> findByUsername(String username) throws IOException {
        Path path = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(path)) return Optional.empty();

        try (Reader reader = Files.newBufferedReader(path);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper
                     .readerFor(EncryptedUserCsvRecord.class)
                     .with(readSchema) // Liest anhand der Kopfzeile
                     .readValues(reader)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord enc = it.next();
                if (enc.getUsername() != null && enc.getUsername().equalsIgnoreCase(username.trim())) {
                    return Optional.of(decryptUser(enc));
                }
            }
        }
        return Optional.empty();
    }

    // =========================================================================
    // 3. ALLE USER STREAMEN (ZUM ANZEIGEN IN DER TABELLE)
    // =========================================================================
    public Stream<UserAccount> streamAllUsers() throws IOException {
        Path path = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(path)) return Stream.empty();

        BufferedReader reader = Files.newBufferedReader(path);
        MappingIterator<EncryptedUserCsvRecord> it = csvMapper
                .readerFor(EncryptedUserCsvRecord.class)
                .with(readSchema) // Liest anhand der Kopfzeile
                .readValues(reader);

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false)
                .map(this::decryptUser)
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException ignored) {}
                });
    }

    // =========================================================================
    // 4. EINTRAG LÖSCHEN (STREAMEND ÜBER TEMPORÄRE DATEI)
    // =========================================================================
//    public synchronized boolean deleteByUsername(String username) throws IOException {
//        Path sourcePath = Paths.get(CSV_FILE_PATH);
//        if (!Files.exists(sourcePath)) return false;
//
//        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
//        boolean deleted = false;
//
//        try (Reader reader = Files.newBufferedReader(sourcePath);
//             MappingIterator<EncryptedUserCsvRecord> it = csvMapper
//                     .readerFor(EncryptedUserCsvRecord.class)
//                     .with(readSchema)
//                     .readValues(reader);
//             OutputStream os = Files.newOutputStream(tempPath);
//             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {
//
//            while (it.hasNext()) {
//                EncryptedUserCsvRecord record = it.next();
//                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(username.trim())) {
//                    deleted = true; // Überspringen = löschen
//                } else {
//                    writer.write(record);
//                }
//            }
//        }
//
//        if (deleted) {
//            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
//        } else {
//            Files.deleteIfExists(tempPath);
//        }
//
//        return deleted;
//    }

    /**
     * Löscht einen Benutzer und hebt bei allen zugehörigen Erben die Verknüpfung auf.
     * Bereinigt zusätzlich das Verzeichnis /data/<username> und den PDF-Brief.
     */
    public synchronized boolean deleteByUsername(String username) throws IOException {
        Path sourcePath = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(sourcePath)) return false;

        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
        boolean deleted = false;
        String targetUser = username.trim();

        try (Reader reader = Files.newBufferedReader(sourcePath);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper
                     .readerFor(EncryptedUserCsvRecord.class)
                     .with(readSchema)
                     .readValues(reader);
             OutputStream os = Files.newOutputStream(tempPath);
             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord record = it.next();

                // 1. ZIEL-USER LÖSCHEN (Überspringen = nicht in neue CSV schreiben)
                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(targetUser)) {
                    deleted = true;
                } else {
                    // 2. KASKADIERENDES UPDATE: Wenn dieser User ein Erbe des gelöschten Erblassers war
                    if (record.getErbeVon() != null && record.getErbeVon().equalsIgnoreCase(targetUser)) {
                        record.setErbeVon(""); // Verknüpfung aufheben
                    }
                    writer.write(record);
                }
            }
        }

        if (deleted) {
            // Atomarer Austausch der CSV-Datei
            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // 3. DATEISYSTEM BEREINIGEN:
            // a) Ordner /data/<username>/ mit allen verschlüsselten Dokumenten löschen
            Path erblasserDataDir = Paths.get("data", targetUser);
            if (Files.exists(erblasserDataDir)) {
                FileSystemUtils.deleteRecursively(erblasserDataDir);
            }

            // b) Generierten DIN 5008 Brief löschen
            String safePdfName = targetUser.replaceAll("[^a-zA-Z_]", "") + ".pdf";
            Path pdfFile = Paths.get("generated-letters", safePdfName);
            Files.deleteIfExists(pdfFile);

        } else {
            Files.deleteIfExists(tempPath);
        }

        return deleted;
    }

    // =========================================================================
    // 5. STATUS-FELD TOGGLEN (ACTIVE / ERBLASSER / VERSTORBEN)
    // =========================================================================
    public synchronized boolean toggleField(String username, String fieldName) throws IOException {
        Path sourcePath = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(sourcePath)) return false;

        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
        boolean updated = false;

        try (Reader reader = Files.newBufferedReader(sourcePath);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper
                     .readerFor(EncryptedUserCsvRecord.class)
                     .with(readSchema)
                     .readValues(reader);
             OutputStream os = Files.newOutputStream(tempPath);
             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord record = it.next();
                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(username.trim())) {
                    switch (fieldName.toLowerCase()) {
                        case "active" -> record.setActive(!record.isActive());
                        case "erblasser" -> record.setErblasser(!record.isErblasser());
                        case "verstorben" -> record.setVerstorben(!record.isVerstorben());
                        case "verwalter" -> record.setVerwalter(!record.isVerwalter());
                    }
                    updated = true;
                }
                writer.write(record);
            }
        }

        if (updated) {
            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } else {
            Files.deleteIfExists(tempPath);
        }

        return updated;
    }

    // =========================================================================
    // KRYPTO-KONVERTIERUNG (POJO <-> CSV DTO)
    // =========================================================================
    private EncryptedUserCsvRecord encryptUser(UserAccount user) {
        EncryptedUserCsvRecord enc = new EncryptedUserCsvRecord();
        enc.setUsername(user.getUsername());
        enc.setActive(user.isActive());
        enc.setErblasser(user.isErblasser());
        enc.setVerstorben(user.isVerstorben());
        enc.setVerwalter(user.isVerwalter());
        enc.setErbeVon(user.getErbeVon() != null ? user.getErbeVon() : "");
        enc.setVerwalterUsername(user.getVerwalterUsername() != null ? user.getVerwalterUsername() : "");
        enc.setTodesdatum(user.getTodesdatum() != null ? user.getTodesdatum().toString() : "");

        enc.setEncFullName(encrypt(user.getFullName()));
        enc.setEncStreet(encrypt(user.getStreet()));
        enc.setEncZipCode(encrypt(user.getZipCode()));
        enc.setEncCity(encrypt(user.getCity()));
        enc.setEncValidUntil(encrypt(user.getValidUntil() != null ? user.getValidUntil().toString() : ""));
        return enc;
    }

    private UserAccount decryptUser(EncryptedUserCsvRecord enc) {
        UserAccount user = new UserAccount();
        user.setUsername(enc.getUsername());
        user.setActive(enc.isActive());
        user.setErblasser(enc.isErblasser());
        user.setVerstorben(enc.isVerstorben());
        user.setVerwalter(enc.isVerwalter());
        user.setErbeVon(enc.getErbeVon());
        user.setVerwalterUsername(enc.getVerwalterUsername());
        if (enc.getTodesdatum() != null && !enc.getTodesdatum().isBlank()) {
            user.setTodesdatum(LocalDate.parse(enc.getTodesdatum()));
        }

        user.setFullName(decrypt(enc.getEncFullName()));
        user.setStreet(decrypt(enc.getEncStreet()));
        user.setZipCode(decrypt(enc.getEncZipCode()));
        user.setCity(decrypt(enc.getEncCity()));
        String validStr = decrypt(enc.getEncValidUntil());
        if (validStr != null && !validStr.isBlank()) {
            user.setValidUntil(LocalDate.parse(validStr));
        }
        return user;
    }

    // =========================================================================
    // AES-GCM BASIS-METHODEN
    // =========================================================================
    private String encrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, this.aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(value.getBytes());

            // Format: [12 Bytes IV] + [Ciphertext + 16 Bytes Auth-Tag]
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Verschlüsselungsfehler", e);
        }
    }

    private String decrypt(String base64Value) {
        if (base64Value == null || base64Value.isEmpty()) return "";
        try {
            byte[] cipherMessage = Base64.getUrlDecoder().decode(base64Value);
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, this.aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText);
        } catch (Exception e) {
            return "[DECRYPT_ERROR]";
        }
    }

    // =========================================================================
    // NEU: ERBLASSER EINEM USER ZUORDNEN (STREAMEND)
    // =========================================================================
    public synchronized boolean assignErblasser(String heirUsername, String erblasserUsername) throws IOException {
        Path sourcePath = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(sourcePath)) return false;

        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
        boolean updated = false;

        try (Reader reader = Files.newBufferedReader(sourcePath);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper
                     .readerFor(EncryptedUserCsvRecord.class)
                     .with(readSchema)
                     .readValues(reader);
             OutputStream os = Files.newOutputStream(tempPath);
             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord record = it.next();
                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(heirUsername.trim())) {
                    record.setErbeVon(erblasserUsername != null ? erblasserUsername.trim() : "");
                    updated = true;
                }
                writer.write(record);
            }
        }

        if (updated) {
            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } else {
            Files.deleteIfExists(tempPath);
        }

        return updated;
    }


    // In EncryptedUserCsvService.java ergänzen:

    /**
     * Aktualisiert die Daten eines bestehenden Benutzers in der CSV-Datei (gestreamt)
     */
    public synchronized boolean updateUser(UserAccount updatedUser) throws IOException {
        Path sourcePath = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(sourcePath)) return false;

        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
        boolean found = false;

        try (Reader reader = Files.newBufferedReader(sourcePath);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper
                     .readerFor(EncryptedUserCsvRecord.class)
                     .with(readSchema)
                     .readValues(reader);
             OutputStream os = Files.newOutputStream(tempPath);
             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord record = it.next();
                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(updatedUser.getUsername().trim())) {
                    // 1. Unverschlüsselte Felder aktualisieren
                    record.setActive(updatedUser.isActive());
                    record.setErblasser(updatedUser.isErblasser());
                    record.setVerstorben(updatedUser.isVerstorben());
                    record.setVerwalter(updatedUser.isVerwalter());
                    if (updatedUser.getErbeVon() != null) {
                        record.setErbeVon(updatedUser.getErbeVon());
                    }

                    // 2. Sensible Felder neu verschlüsseln (AES-256-GCM)
                    record.setEncFullName(encrypt(updatedUser.getFullName()));
                    record.setEncStreet(encrypt(updatedUser.getStreet()));
                    record.setEncZipCode(encrypt(updatedUser.getZipCode()));
                    record.setEncCity(encrypt(updatedUser.getCity()));
                    record.setEncValidUntil(encrypt(updatedUser.getValidUntil() != null ? updatedUser.getValidUntil().toString() : ""));

                    writer.write(record);
                    found = true;
                } else {
                    writer.write(record);
                }
            }
        }

        if (found) {
            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } else {
            Files.deleteIfExists(tempPath);
        }

        return found;
    }

    /**
     * Verwalter einem Erblasser zuordnen (mit Kollisionsprüfung: Verwalter darf nicht Erbe sein!)
     */
    public synchronized boolean assignVerwalter(String erblasserUsername, String verwalterUsername) throws IOException {
        Path sourcePath = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(sourcePath)) return false;

        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
        boolean updated = false;

        try (Reader reader = Files.newBufferedReader(sourcePath);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper.readerFor(EncryptedUserCsvRecord.class).with(readSchema).readValues(reader);
             OutputStream os = Files.newOutputStream(tempPath);
             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord record = it.next();
                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(erblasserUsername.trim())) {
                    record.setVerwalterUsername(verwalterUsername != null ? verwalterUsername.trim() : "");
                    updated = true;
                }
                writer.write(record);
            }
        }
        if (updated) {
            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } else {
            Files.deleteIfExists(tempPath);
        }
        return updated;
    }

    /**
     * Feststellen des Todesfalls durch den Verwalter
     */
    public synchronized boolean declareDeceased(String erblasserUsername, LocalDate todesdatum) throws IOException {
        Path sourcePath = Paths.get(CSV_FILE_PATH);
        if (!Files.exists(sourcePath)) return false;

        Path tempPath = Paths.get(CSV_FILE_PATH + ".tmp");
        boolean updated = false;

        try (Reader reader = Files.newBufferedReader(sourcePath);
             MappingIterator<EncryptedUserCsvRecord> it = csvMapper.readerFor(EncryptedUserCsvRecord.class).with(readSchema).readValues(reader);
             OutputStream os = Files.newOutputStream(tempPath);
             SequenceWriter writer = csvMapper.writer(writeSchema).writeValues(os)) {

            while (it.hasNext()) {
                EncryptedUserCsvRecord record = it.next();
                if (record.getUsername() != null && record.getUsername().equalsIgnoreCase(erblasserUsername.trim())) {
                    record.setVerstorben(true);
                    record.setTodesdatum(todesdatum.toString());
                    updated = true;
                }
                writer.write(record);
            }
        }
        if (updated) {
            Files.move(tempPath, sourcePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } else {
            Files.deleteIfExists(tempPath);
        }
        return updated;
    }

}
