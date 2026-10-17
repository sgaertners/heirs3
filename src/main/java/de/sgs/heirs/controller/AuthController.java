package de.sgs.heirs.controller;


import de.sgs.heirs.service.ServerMasterKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AuthController {

    private final ServerMasterKeyService masterKeyService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(ServerMasterKeyService masterKeyService,
                          SecurityContextRepository securityContextRepository) {
        this.masterKeyService = masterKeyService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/master")
    public String loginPage() {
        return "master";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "/admin/dashboard";
    }

    @GetMapping("/user-dashboard")
    public String userdashboard(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "user-dashboard";
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

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(auth);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return "redirect:/user-dashboard";

        } catch (Exception e) {
            return "redirect:/user-dashboard?error=auth_failed";
        }
    }

}