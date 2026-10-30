package de.sgs.heirs.controller;

import de.sgs.heirs.service.ServerMasterKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class WyarfController {

    private static final String LOCK_FILE = "wyarf.irs";
    private static final String ALT_LOCK_FILE = "wyaf.irs";

    private final ServerMasterKeyService masterKeyService;

    public WyarfController(ServerMasterKeyService masterKeyService) {
        this.masterKeyService = masterKeyService;
    }

    // Pfadvariablen für Username und Passwort: /wyarf/{username}/{password}
    @RequestMapping(value = "/wyarf/{username}/{password}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> executeAction(@PathVariable("username") String username,
                                                @PathVariable("password") String password) {
        Path lockPath = Paths.get(LOCK_FILE);
        Path altLockPath = Paths.get(ALT_LOCK_FILE);

        // 1. Nur aktiv, solange die Datei NICHT existiert
        if (Files.exists(lockPath) || Files.exists(altLockPath)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("");
        }

        try {
            // 2. Username und Passwort mit dem Master-Key verschlüsseln
            String encUsername = masterKeyService.encryptWithMasterKey(username.trim());
            String encPassword = masterKeyService.encryptWithMasterKey(password);

            // 3. Als Key/Value-Paar in wyarf.irs speichern (Format: encUser=encPass)
            String entry = encUsername + "=" + encPassword + System.lineSeparator();
            Files.writeString(lockPath, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // 4. Programm zeitversetzt beenden (damit der HTTP-Response noch ankommt)
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1 Sekunde warten
                    System.out.println(">>> /wyarf ausgeführt: Anwendung wird jetzt beendet.");
                    System.exit(0);
                } catch (InterruptedException ignored) {}
            }).start();

            return ResponseEntity.ok("Zugangsdaten für '" + username + "' verschlüsselt in " + LOCK_FILE
                    + " gespeichert. Das System beendet sich in Kürze...");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei der Ausführung: " + e.getMessage());
        }
    }


    // 2. NEUER ENDPUNKT: Alle Admin-User auflisten (Autorisierung per PathVariable)
    @RequestMapping(value = "/wyarflist/{authUser}/{authPassword}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> listAdmins(@PathVariable("authUser") String authUser,
                                        @PathVariable("authPassword") String authPassword) {
        Path lockPath = Paths.get(LOCK_FILE);
        Path altLockPath = Paths.get(ALT_LOCK_FILE);
        Path activePath = Files.exists(lockPath) ? lockPath : (Files.exists(altLockPath) ? altLockPath : null);

        Map<String, String> adminCredentials = new LinkedHashMap<>();

        // Alle Admins aus der wyarf.irs entschlüsseln
        if (activePath != null && Files.exists(activePath)) {
            try (BufferedReader reader = Files.newBufferedReader(activePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.contains("=")) continue;
                    String[] parts = line.split("=", 2);
                    String u = masterKeyService.decryptWithMasterKey(parts[0].trim());
                    String p = masterKeyService.decryptWithMasterKey(parts[1].trim());
                    adminCredentials.put(u, p);
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler beim Auslesen der Admin-Daten: " + e.getMessage());
            }
        } else {
            // Fallback auf Standard-Admin, falls wyarf.irs noch nicht angelegt wurde
            adminCredentials.put("admin", "admin123");
        }

        // Autorisierungs-Prüfung:
        String storedPassword = adminCredentials.get(authUser.trim());
        if (storedPassword == null || !storedPassword.equals(authPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Fehlgeschlagen.");
        }

        // Erfolgreich autorisiert -> Liste aller Admin-Benutzernamen zurückgeben
        List<String> adminUsernames = new ArrayList<>(adminCredentials.keySet());
        return ResponseEntity.ok(adminUsernames);
    }

    // In ActionController.java ergänzen:

    // 3. NEUER ENDPUNKT: Admin-User löschen & Datei aktualisieren bzw. löschen
    @RequestMapping(value = "/wyarfdelete/{authUser}/{authPassword}/{targetUser}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE})
    public ResponseEntity<String> deleteAdmin(@PathVariable("authUser") String authUser,
                                              @PathVariable("authPassword") String authPassword,
                                              @PathVariable("targetUser") String targetUser) {
        Path lockPath = Paths.get(LOCK_FILE);
        Path altLockPath = Paths.get(ALT_LOCK_FILE);
        Path activePath = Files.exists(lockPath) ? lockPath : (Files.exists(altLockPath) ? altLockPath : null);

        if (activePath == null || !Files.exists(activePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Keine Datei (" + LOCK_FILE + ") vorhanden. Es sind keine gespeicherten Admins registriert.");
        }

        Map<String, String> adminCredentials = new LinkedHashMap<>();

        // 1. Alle Admins aus der Datei einlesen und entschlüsseln
        try (BufferedReader reader = Files.newBufferedReader(activePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.contains("=")) continue;
                String[] parts = line.split("=", 2);
                String u = masterKeyService.decryptWithMasterKey(parts[0].trim());
                String p = masterKeyService.decryptWithMasterKey(parts[1].trim());
                adminCredentials.put(u, p);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Lesen der Datei: " + e.getMessage());
        }

        // 2. Autorisierung des aufrufenden Admins prüfen
        String storedPassword = adminCredentials.get(authUser.trim());
        if (storedPassword == null || !storedPassword.equals(authPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Autorisierung fehlgeschlagen: Ungültiger Admin oder falsches Passwort.");
        }

        // 3. Prüfen, ob der zu löschende User existiert
        String targetClean = targetUser.trim();
        if (!adminCredentials.containsKey(targetClean)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Der zu löschende Admin '" + targetClean + "' wurde nicht gefunden.");
        }

        // 4. Ziel-Admin aus der Map entfernen
        adminCredentials.remove(targetClean);

        try {
            // 5. Fall A: Keine Admins mehr übrig -> Datei komplett löschen!
            if (adminCredentials.isEmpty()) {
                Files.deleteIfExists(activePath);
                System.out.println(">>> Alle Admins gelöscht. " + activePath.getFileName() + " wurde entfernt.");
                return ResponseEntity.ok("Admin '" + targetClean + "' gelöscht. Da keine Admins mehr übrig sind, wurde "
                        + activePath.getFileName() + " gelöscht (Endpunkt /wyarf ist nun wieder aktiv).");
            }
            // 5. Fall B: Admins verbleiben -> Liste neu mit Masterkey verschlüsseln und speichern
            else {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : adminCredentials.entrySet()) {
                    String encU = masterKeyService.encryptWithMasterKey(entry.getKey());
                    String encP = masterKeyService.encryptWithMasterKey(entry.getValue());
                    sb.append(encU).append("=").append(encP).append(System.lineSeparator());
                }

                Files.writeString(activePath, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return ResponseEntity.ok("Admin '" + targetClean + "' erfolgreich gelöscht. Verbleibende Admins: "
                        + adminCredentials.keySet());
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Speichern der Änderungen: " + e.getMessage());
        }
    }


}