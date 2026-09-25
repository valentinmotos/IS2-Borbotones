package com.zero.ecommerce.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminInicioController {

    @GetMapping("/admin")
    public String inicio(Model model) {
        model.addAttribute("pageTitle", "Panel administrativo");
        model.addAttribute("menuActivo", "inicio");
        return "dev/ejemplo-admin";
    }
}
