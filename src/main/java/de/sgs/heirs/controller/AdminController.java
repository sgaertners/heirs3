package de.sgs.heirs.controller;


import de.sgs.heirs.service.Din5008PdfService;
import de.sgs.heirs.service.QrCodeService;
import de.sgs.heirs.service.ServerMasterKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ServerMasterKeyService masterKeyService;
    private final QrCodeService qrCodeService;
    private final Din5008PdfService pdfService;

    public AdminController(ServerMasterKeyService masterKeyService,
                           QrCodeService qrCodeService,
                           Din5008PdfService pdfService) {
        this.masterKeyService = masterKeyService;
        this.qrCodeService = qrCodeService;
        this.pdfService = pdfService;
    }

    @GetMapping("/create-user")
    public String showCreateUserForm() {
        return "admin/create-user";
    }

    @PostMapping("/create-user")
    public String createUserAndGenerateQr(@RequestParam("username") String username,
                                          @RequestParam(value = "validYears", defaultValue = "1") int validYears,
                                          HttpServletRequest request,
                                          Model model) {
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Benutzername ist ein Pflichtfeld!");
            return "admin/create-user";
        }

        String sanitizedUsername = username.trim();
        LocalDate validUntil = LocalDate.now().plusYears(validYears);

        try {
            // 1. Server signiert den Ausweis mit dem Master-Private-Key
            String signature = masterKeyService.signBadge(sanitizedUsername, validUntil);

            // 2. URL für den QR-Code zusammenstellen (enthält Username, Gültigkeit & Signatur)
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null).build().toUriString();

            String loginUrl = String.format("%s/qr-auth?u=%s&exp=%s&sig=%s",
                    baseUrl,
                    sanitizedUsername,
                    validUntil,
                    signature
            );

            // 3. QR-Code rendern
            BufferedImage qrImage = qrCodeService.generateQrBufferedImage(loginUrl, 300, 300);

            // 4. Automatisch DIN 5008 Brief generieren
            File pdfFile = pdfService.createLetterPdf(sanitizedUsername, validUntil, qrImage);

            String qrImageBase64 = qrCodeService.generateQrCodeBase64(loginUrl, 300, 300);
            model.addAttribute("username", sanitizedUsername);
            model.addAttribute("validUntil", validUntil);
            model.addAttribute("qrImage", qrImageBase64);
            model.addAttribute("pdfFilename", pdfFile.getName());
            model.addAttribute("generated-letters", "file://generated-letters");

            return "admin/show-qr";

        } catch (Exception e) {
            model.addAttribute("error", "Fehler bei der Ausweiserstellung: " + e.getMessage());
            return "admin/create-user";
        }
    }

}