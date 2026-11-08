package de.sgs.heirs.config;

import de.sgs.heirs.model.NotarStammdatenConfig;
import de.sgs.heirs.service.NotarStammdatenService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final NotarStammdatenService stammdatenService;

    public GlobalControllerAdvice(NotarStammdatenService stammdatenService) {
        this.stammdatenService = stammdatenService;
    }

    @ModelAttribute("notarConfig")
    public NotarStammdatenConfig getNotarConfig() {
        return stammdatenService.getConfig();
    }
}