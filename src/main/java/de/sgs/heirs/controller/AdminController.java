package de.sgs.heirs.controller;


import de.sgs.heirs.model.UserAccount;
import de.sgs.heirs.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ServerMasterKeyService masterKeyService;
    private final QrCodeService qrCodeService;
    private final Din5008PdfService pdfService;
    private final EncryptedUserCsvService csvService;
    private final AuditService auditService;

    public AdminController(ServerMasterKeyService masterKeyService,
                           QrCodeService qrCodeService,
                           Din5008PdfService pdfService,
                           EncryptedUserCsvService csvService, AuditService auditService) {
        this.masterKeyService = masterKeyService;
        this.qrCodeService = qrCodeService;
        this.pdfService = pdfService;
        this.csvService = csvService;
        this.auditService = auditService;
    }

    // --- DASHBOARD ---
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Dashboard requested");
        return "admin/dashboard";
    }

    // --- FORMULAR: USER ANLEGEN ---
    @GetMapping("/create-user")
    public String showCreateUserForm() {
        log.info("Create user form requested");
        return "admin/create-user";
    }

    @PostMapping("/create-user")
    public String createUserAndGenerateQr(@RequestParam("username") String username,
                                          @RequestParam(value = "fullName", required = false) String fullName,
                                          @RequestParam(value = "street", required = false) String street,
                                          @RequestParam(value = "zipCode", required = false) String zipCode,
                                          @RequestParam(value = "city", required = false) String city,
                                          @RequestParam(value = "validYears", defaultValue = "1") int validYears,
                                          @RequestParam(value = "erblasser", defaultValue = "false") boolean erblasser, // <-- NEU
                                          HttpServletRequest request,
                                          Model model) {

        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Benutzername ist ein Pflichtfeld!");
            return "admin/create-user";
        }

        log.info("Create user form submitted: {}", username);

        String sanitizedUsername = username.trim();
        LocalDate validUntil = LocalDate.now().plusYears(validYears);

        auditService.log(sanitizedUsername, "ACCOUNT_CREATED",
                String.format("by=admin; erblasser=%b; validUntil=%s", erblasser, validUntil));

        try {
            // 1. In die verschlüsselte CSV streamen
            UserAccount account = new UserAccount(sanitizedUsername, fullName, street, zipCode, city, validUntil);
            account.setErblasser(erblasser); // <-- Status setzen!
            csvService.saveUser(account);

            // 2. Signatur & Login-URL erstellen
            String signature = masterKeyService.signBadge(sanitizedUsername, validUntil);
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).build().toUriString();
            String loginUrl = String.format("%s/qr-auth?u=%s&exp=%s&sig=%s", baseUrl, sanitizedUsername, validUntil, signature);
            String erbeVon = account.getErbeVon() != null ? account.getErbeVon() : erblasser ? "Erblasser" : "Erbner";

            // 3. QR-Code & DIN 5008 Brief generieren
            BufferedImage qrImage = qrCodeService.generateQrBufferedImage(loginUrl, 300, 300);
            File pdfFile = pdfService.createLetterPdf(sanitizedUsername, fullName, street, zipCode, city, erbeVon, validUntil, qrImage);

            String qrImageBase64 = qrCodeService.generateQrCodeBase64(loginUrl, 300, 300);
            model.addAttribute("username", sanitizedUsername);
            model.addAttribute("validUntil", validUntil);
            model.addAttribute("qrImage", qrImageBase64);
            model.addAttribute("pdfFilename", pdfFile.getName());

            return "admin/show-qr";

        } catch (Exception e) {
            model.addAttribute("error", "Fehler: " + e.getMessage());
            return "admin/create-user";
        }
    }


    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam("username") String username, RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = csvService.deleteByUsername(username);
            auditService.log(username, "ACCOUNT_DELETED", "by=admin");
            log.info("User '{}' deleted", username);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success",
                        "Benutzer '" + username + "' wurde gelöscht. Zuordnungen bei zugehörigen Erben sowie hinterlegte Dokumente wurden automatisch bereinigt.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Benutzer konnte nicht gefunden werden.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Löschen: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }


    // --- PDF DOWNLOAD / VORSCHAU ---
    @GetMapping("/download-pdf/{filename}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String filename) {
        log.info("PDF download requested: {}", filename);

        String safeFilename = filename.replaceAll("[^a-zA-Z0-9_\\.]", "");
        File file = new File("generated-letters", safeFilename);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFilename + "\"")
                .body(resource);
    }

    // POST-Route zum Umschalten eines Feldes
    @PostMapping("/users/toggle")
    public String toggleUserField(@RequestParam("username") String username,
                                  @RequestParam("field") String field,
                                  RedirectAttributes redirectAttributes) {
        try {
            log.info("Toggle user field requested: username={}, field={}", username, field);
            boolean success = csvService.toggleField(username, field);
            auditService.log(username, "STATUS_TOGGLED", "field=" + field.toLowerCase());
            if (success) {
                redirectAttributes.addFlashAttribute("success", "Status '" + field + "' für " + username + " aktualisiert.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Benutzer nicht gefunden.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Aktualisieren: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // 1. In listUsers: Erblasser-Liste für das Modal mitgeben
    @GetMapping("/users")
    public String listUsers(Model model) throws IOException {
        try (Stream<UserAccount> userStream = csvService.streamAllUsers()) {
            List<UserAccount> users = userStream.toList();
            log.info("Users list requested");

            // Alle Benutzer filtern, die Erblasser sind
            List<UserAccount> availableErblasser = users.stream()
                    .filter(UserAccount::isErblasser)
                    .toList();

            model.addAttribute("users", users);
            model.addAttribute("availableErblasser", availableErblasser);
            model.addAttribute("now", LocalDate.now());
        }
        return "admin/users";
    }

    @PostMapping("/users/assign-erblasser")
    public String assignErblasser(@RequestParam("heirUsername") String heirUsername,
                                  @RequestParam(value = "erblasserUsername", required = false) String erblasserUsername,
                                  RedirectAttributes redirectAttributes) {
        try {
            csvService.assignErblasser(heirUsername, erblasserUsername);
            auditService.log(heirUsername, "ERBLASSER_ASSIGNED", "erblasser=" + erblasserUsername);
            log.info("Erblasser assignment for user '{}' successful", heirUsername);
            redirectAttributes.addFlashAttribute("success", "Erblasser-Zuordnung für " + heirUsername + " erfolgreich gespeichert.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Zuordnung: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }


    // 1. Bearbeiten-Formular aufrufen
    @GetMapping("/users/edit/{username}")
    public String showEditUserForm(@PathVariable String username, Model model, RedirectAttributes redirectAttributes) throws IOException {
        log.info("Edit user form requested: username={}", username);
        Optional<UserAccount> userOpt = csvService.findByUsername(username);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Benutzer '" + username + "' wurde nicht gefunden.");
            return "redirect:/admin/users";
        }

        model.addAttribute("user", userOpt.get());
        return "admin/edit-user";
    }

    // 2. Änderungen speichern
    @PostMapping("/users/edit")
    public String processEditUser(@RequestParam("username") String username,
                                  @RequestParam(value = "fullName", required = false) String fullName,
                                  @RequestParam(value = "street", required = false) String street,
                                  @RequestParam(value = "zipCode", required = false) String zipCode,
                                  @RequestParam(value = "city", required = false) String city,
                                  @RequestParam("validUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                                  @RequestParam(value = "active", defaultValue = "false") boolean active,
                                  @RequestParam(value = "erblasser", defaultValue = "false") boolean erblasser,
                                  @RequestParam(value = "verstorben", defaultValue = "false") boolean verstorben,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            log.info("User update requested: username={}", username);
            Optional<UserAccount> existingOpt = csvService.findByUsername(username);
            if (existingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Benutzer nicht gefunden.");
                return "redirect:/admin/users";
            }

            auditService.log(username, "ACCOUNT_UPDATED", String.format("erblasser=%b", erblasser));

            UserAccount user = existingOpt.get();
            user.setFullName(fullName);
            user.setStreet(street);
            user.setZipCode(zipCode);
            user.setCity(city);
            user.setValidUntil(validUntil);
            user.setActive(active);
            user.setErblasser(erblasser);
            user.setVerstorben(verstorben);

            // 1. In der verschlüsselten CSV aktualisieren
            csvService.updateUser(user);

            // 2. DIN 5008 Brief & QR-Code mit den neuen Daten neu generieren
            String signature = masterKeyService.signBadge(username, validUntil);
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).build().toUriString();
            String loginUrl = String.format("%s/qr-auth?u=%s&exp=%s&sig=%s", baseUrl, username, validUntil, signature);
            String erbeVon = user.getErbeVon() != null ? user.getErbeVon() : "Erblasser";

            BufferedImage qrImage = qrCodeService.generateQrBufferedImage(loginUrl, 300, 300);
            pdfService.createLetterPdf(username, fullName, street, zipCode, city, erbeVon, validUntil, qrImage);

            redirectAttributes.addFlashAttribute("success", "Daten für '" + username + "' erfolgreich aktualisiert und Brief neu generiert.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler beim Aktualisieren: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    // In AdminController.java ergänzen:

    /**
     * Generiert den DIN-5008-Brief für einen bestehenden Benutzer komplett neu
     */
    @PostMapping("/users/refresh-letter")
    public String refreshLetter(@RequestParam("username") String username,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Optional<UserAccount> userOpt = csvService.findByUsername(username.trim());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Benutzer '" + username + "' nicht gefunden.");
                return "redirect:/admin/users";
            }

            UserAccount user = userOpt.get();

            // 1. Frische Signatur mit aktuellem Zeitstempel und hinterlegtem Gültigkeitsdatum
            LocalDate validUntil = user.getValidUntil() != null ? user.getValidUntil() : LocalDate.now().plusYears(1);
            String signature = masterKeyService.signBadge(user.getUsername(), validUntil);

            // 2. Login-URL für QR-Code neu zusammensetzen
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).build().toUriString();
            String loginUrl = String.format("%s/qr-auth?u=%s&exp=%s&sig=%s",
                    baseUrl,
                    user.getUsername(),
                    validUntil,
                    signature
            );

            // 3. QR-Code & DIN 5008 PDF mit aktuellen Daten (und SenderInfo) neu erzeugen
            BufferedImage qrImage = qrCodeService.generateQrBufferedImage(loginUrl, 300, 300);
            File pdfFile = pdfService.createLetterPdf(
                    user.getUsername(),
                    user.getFullName(),
                    user.getStreet(),
                    user.getZipCode(),
                    user.getCity(),
                    user.getErbeVon(),
                    validUntil,
                    qrImage
            );

            // 4. Audit-Log schreiben
            auditService.log(user.getUsername(), "LETTER_REFRESHED", "pdf=" + pdfFile.getName());

            redirectAttributes.addFlashAttribute("success",
                    "DIN-5008-Brief für '" + username + "' wurde erfolgreich aktualisiert!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Fehler beim Aktualisieren des Briefs: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

}