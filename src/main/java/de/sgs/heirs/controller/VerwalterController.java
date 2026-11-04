package de.sgs.heirs.controller;

import de.sgs.heirs.model.UserAccount;
import de.sgs.heirs.service.AuditService;
import de.sgs.heirs.service.EncryptedUserCsvService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/verwalter")
public class VerwalterController {

    private final EncryptedUserCsvService csvService;
    private final AuditService auditService;

    public VerwalterController(EncryptedUserCsvService csvService, AuditService auditService) {
        this.csvService = csvService;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) throws IOException {
        String verwalterUsername = authentication.getName();
        model.addAttribute("username", verwalterUsername);

        // 1. Verwalter-Account laden
        UserAccount currentVerwalter = csvService.findByUsername(verwalterUsername).orElse(null);
        String verwalterErbvater = (currentVerwalter != null) ? currentVerwalter.getErbeVon() : null;

        // 2. Nur Erblasser filtern, bei denen DIESER Verwalter eingetragen ist
        // SICHERHEITSREGEL: Der Verwalter darf NICHT selbst Erbe des jeweiligen Erblassers sein!
        List<UserAccount> betreuteErblasser = csvService.streamAllUsers()
                .filter(UserAccount::isErblasser)
                .filter(u -> verwalterUsername.equalsIgnoreCase(u.getVerwalterUsername()))
                .filter(u -> verwalterErbvater == null || !verwalterErbvater.equalsIgnoreCase(u.getUsername()))
                .toList();

        model.addAttribute("erblasserList", betreuteErblasser);
        model.addAttribute("today", LocalDate.now());
        return "verwalter/dashboard";
    }

    @PostMapping("/declare-deceased")
    public String declareDeceased(@RequestParam("erblasser") String erblasser,
                                  @RequestParam("todesdatum") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate todesdatum,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        String verwalterUsername = authentication.getName();

        try {
            // Validierung: Todesdatum darf nicht in der Zukunft liegen
            if (todesdatum.isAfter(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("error", "Das Todesdatum darf nicht in der Zukunft liegen!");
                return "redirect:/verwalter/dashboard";
            }

            // Kollisionsprüfung wiederholen
            UserAccount currentVerwalter = csvService.findByUsername(verwalterUsername).orElse(null);
            if (currentVerwalter != null && erblasser.equalsIgnoreCase(currentVerwalter.getErbeVon())) {
                redirectAttributes.addFlashAttribute("error", "Interessenkonflikt: Sie sind als Erbe dieses Erblassers eingetragen!");
                return "redirect:/verwalter/dashboard";
            }

            // Todesfall verbindlich feststellen
            csvService.declareDeceased(erblasser, todesdatum);
            auditService.log(verwalterUsername, "DEATH_DECLARED",
                    String.format("erblasser=%s; todesdatum=%s", erblasser, todesdatum));

            redirectAttributes.addFlashAttribute("success",
                    "Der Todesfall für Erblasser '" + erblasser + "' wurde verbindlich mit Datum " + todesdatum + " beurkundet.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Beurkundung: " + e.getMessage());
        }

        return "redirect:/verwalter/dashboard";
    }
}
