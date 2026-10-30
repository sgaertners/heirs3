package de.sgs.heirs.controller;


import de.sgs.heirs.model.FileEntry;
import de.sgs.heirs.model.UserAccount;
import de.sgs.heirs.service.AuditService;
import de.sgs.heirs.service.EncryptedUserCsvService;
import de.sgs.heirs.service.ErblasserFileService;
import de.sgs.heirs.service.ServerMasterKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final ServerMasterKeyService masterKeyService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final EncryptedUserCsvService csvService;
    private final ErblasserFileService fileService;
    private final AuditService auditService;

    // Vordefinierte Kategorien
    private static final List<String> CATEGORIES = List.of(
            "Testament & Verfügungen",
            "Immobilien",
            "Banken & Finanzen",
            "Versicherungen",
            "Verträge & Abos",
            "Persönliches"
    );

    public AuthController(ServerMasterKeyService masterKeyService,
                          SecurityContextRepository securityContextRepository, EncryptedUserCsvService csvService, ErblasserFileService fileService, AuditService auditService) {
        this.masterKeyService = masterKeyService;
        this.securityContextRepository = securityContextRepository;
        this.csvService = csvService;
        this.fileService = fileService;
        this.auditService = auditService;
    }

    @GetMapping("/master")
    public String loginPage() {
        log.info("Login page requested");
        return "master";
    }

    @GetMapping("/logout")
    public String logoutPage() {
        log.info("Logout page requested");
        return "logout";
    }

    // 1. Dashboard-Route erweitern:
    @GetMapping({"/dashboard", "/user-dashboard"})
    public String dashboard(Authentication authentication, Model model) throws IOException {
        String username = authentication.getName();
        model.addAttribute("username", username);
        log.info("Dashboard page requested for user: {}", username);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return "admin/dashboard";
        }

        // 1. User aus der CSV laden (case-insensitive)
        Optional<UserAccount> currentUserOpt = csvService.findByUsername(username.trim());

//        boolean isErblasser = false;
        boolean isErblasser = currentUserOpt.map(UserAccount::isErblasser).orElse(false);
        String erbeVon = currentUserOpt.map(UserAccount::getErbeVon).orElse(null);

        if (currentUserOpt.isPresent()) {
            UserAccount user = currentUserOpt.get();
            isErblasser = user.isErblasser();
            log.info(">>> DASHBOARD-CHECK: User '" + username + "' gefunden! isErblasser=" + isErblasser);
        } else {
            log.info(">>> DASHBOARD-WARNUNG: User '" + username + "' NICHT in users_secure.csv gefunden!");
        }

        // 2. Werte an das Model übergeben
        model.addAttribute("isErblasser", isErblasser);
        model.addAttribute("erbeVon", erbeVon);

        // 1. Dokumente für den ERBEN laden und NACH KATEGORIEN GRUPPIEREN
        if (erbeVon != null && !erbeVon.isBlank()) {
            List<FileEntry> myInheritedFiles = fileService.listFilesForHeir(username, erbeVon);
            Map<String, List<FileEntry>> filesByCategory = myInheritedFiles.stream()
                    .collect(Collectors.groupingBy(FileEntry::getCategory));
            model.addAttribute("filesByCategory", filesByCategory);
        }

        if (isErblasser) {
            // Alle Erben laden, die DIESEM Erblasser zugeordnet sind
            List<UserAccount> assignedHeirs = csvService.streamAllUsers()
                    .filter(u -> username.trim().equalsIgnoreCase(u.getErbeVon() != null ? u.getErbeVon().trim() : ""))
                    .toList();

            model.addAttribute("assignedHeirs", assignedHeirs);
            model.addAttribute("categories", CATEGORIES);
            model.addAttribute("fileList", fileService.listFilesForErblasser(username.trim()));
        }

        return "user-dashboard";
    }

    // 2. Upload-Route hinzufügen:
    @PostMapping("/user/upload")
    public String uploadFile(@RequestParam("heir") String heir,
                             @RequestParam("category") String category,
                             @RequestParam("file") MultipartFile file,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Bitte wähle eine Datei aus!");
            return "redirect:/dashboard";
        }
        log.info("File upload requested: heir={}, category={}, file={}, user={}",
                heir, category, file.getOriginalFilename(), authentication.getName());

        try {
            String erblasser = authentication.getName();
            fileService.saveEncryptedFile(erblasser, heir, category, file);
            redirectAttributes.addFlashAttribute("success", "Datei '" + file.getOriginalFilename() + "' erfolgreich verschlüsselt gespeichert.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Upload: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    /**
     * Ausweis-Prüfung bei Scan mit der Handy-Kamera
     */
    @GetMapping("/qr-auth")
    public String authenticateBadge(@RequestParam("u") String username,
                                    @RequestParam("exp") String expDateStr,
                                    @RequestParam("sig") String signature,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        try {
            LocalDate validUntil = LocalDate.parse(expDateStr);
            log.info("QR authentication requested for user: {}, validUntil: {}", username, validUntil);

            auditService.log(username, "LOGIN_QR", "status=SUCCESS; validUntil=" + validUntil);

            // 1. Prüfen, ob der Ausweis abgelaufen ist
            if (LocalDate.now().isAfter(validUntil)) {
                return "redirect:/user-dashboard?error=expired";
            }

            // 2. Prüfen, ob die Signatur vom Master-Key des Servers stammt
            boolean valid = masterKeyService.verifyBadge(username, validUntil, signature);
            if (!valid) {
                return "redirect:/user-dashboard?error=invalid_signature";
            }

            // 3. Authentifizierung erfolgreich -> Login-Session setzen
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            // In AuthController.java vor dem Setzen des SecurityContext:
            Optional<UserAccount> userOpt = csvService.findByUsername(username);
            if (userOpt.isPresent() && !userOpt.get().isActive()) {
                return "redirect:/login?error=inactive"; // Inaktive Nutzer abweisen
            }

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(auth);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            auditService.log(username, "LOGIN_QR", "status=SUCCESS; validUntil=" + validUntil);

            return "redirect:/dashboard";

        } catch (Exception e) {
            return "redirect:/user-dashboard?error=auth_failed";
        }
    }

    /**
     * Entschlüsselt die Datei im RAM und liefert sie an den Browser aus
     */
    @GetMapping("/user/file/download")
    public ResponseEntity<byte[]> downloadDecryptedFile(@RequestParam("erblasser") String erblasser,
                                                        @RequestParam("heir") String heir,
                                                        @RequestParam("filename") String filename,
                                                        Authentication authentication) {
        String currentUser = authentication.getName();
        log.info("File download requested: heir={}, filename={}, user={}", heir, filename, currentUser);

        // Sicherheitscheck: Nur der Erblasser selbst oder der berechtigte Erbe dürfen abrufen
        if (!currentUser.equalsIgnoreCase(erblasser) && !currentUser.equalsIgnoreCase(heir)) {
            return ResponseEntity.status(403).build();
        }

        try {
            // 1. Datei mit dem Erblasser-Key entschlüsseln
            byte[] decryptedData = fileService.decryptFile(erblasser, heir, filename);

            // 2. MIME-Type ermitteln
            String mimeType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(filename));
            if (mimeType == null) mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            currentUser = authentication.getName();
            // Loggt den Abruf beim abrufenden Benutzer:
            auditService.log(currentUser, "FILE_DECRYPT_READ",
                    String.format("erblasser=%s; heir=%s; filename=%s", erblasser, heir, filename));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(decryptedData);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }


}