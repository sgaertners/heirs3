package de.sgs.heirs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InformationsController {

    @GetMapping("/testament")
    public String info() {
        return "testament";
    }
}
