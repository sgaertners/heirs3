package de.sgs.heirs.controller;

import de.sgs.heirs.service.AuditService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class GlobalErrorController implements ErrorController {

    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    private final AuditService auditService;

    public GlobalErrorController(AuditService auditService) {
        this.auditService = auditService;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response) {
        log.error("Error page requested: status={}, user={}", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
                SecurityContextHolder.getContext().getAuthentication().getName());
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (status != null) ? Integer.parseInt(status.toString()) : 500;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");

        // Harmlosen 404 (z. B. favicon.ico nicht gefunden) IGNORIEREN – KEIN LOGOUT!
        if (statusCode == 404) {
            log.warn("404 page requested: status={}, user={}", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
                    SecurityContextHolder.getContext().getAuthentication().getName());
            return isLoggedIn ? "redirect:/dashboard" : "redirect:/master";
        }

        // Nur bei ECHTEN Server-Fehlern (500 etc.) ausloggen:
        if (isLoggedIn) {
            log.warn("500 page requested: status={}, user={}", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
                    SecurityContextHolder.getContext().getAuthentication().getName());
            auditService.log(auth.getName(), "AUTO_LOGOFF_HTTP_ERROR", "httpStatus=" + statusCode);
            logoutHandler.logout(request, response, auth);
            SecurityContextHolder.clearContext();
            return "redirect:/master?error=http_error&status=" + statusCode;
        }

        return "redirect:/master";
    }
}