package de.sgs.heirs.config;

import de.sgs.heirs.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Unexpected error occurred", ex);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            auditService.log(username, "AUTO_LOGOFF_ERROR", "exception=" + ex.getClass().getSimpleName());

            logoutHandler.logout(request, response, auth);
            SecurityContextHolder.clearContext();
            return "redirect:/master?error=unexpected_error";
        }

        return "redirect:/master";
    }
}