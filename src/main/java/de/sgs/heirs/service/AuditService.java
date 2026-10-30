package de.sgs.heirs.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuditService {

    private static final String AUDIT_BASE_DIR = "audit";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Schreibt einen Audit-Eintrag in die benutzerspezifische Logdatei: /audit/<username>/actions.log
     *
     * WICHTIG: 'details' darf niemals Adressen, Klarnamen oder Dateiinhalte enthalten!
     */
    public synchronized void log(String username, String action, String details) {
        if (username == null || username.isBlank()) return;

        String safeUsername = username.trim().toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_");

        try {
            // Verzeichnis /audit/<username>/ anlegen
            Path userAuditDir = Paths.get(AUDIT_BASE_DIR, safeUsername);
            Files.createDirectories(userAuditDir);

            Path logFile = userAuditDir.resolve("actions.log");

            // Format: [TIMESTAMP] | [ACTION] | [METADATA]
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String logLine = String.format("[%s] | %-18s | %s%n", timestamp, action.toUpperCase(), details != null ? details : "-");

            Files.writeString(logFile, logLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            System.err.println("Audit-Logging fehlgeschlagen für " + username + ": " + e.getMessage());
        }
    }
}