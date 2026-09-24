package com.zero.ecommerce.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DevController {

    @GetMapping("/dev/ejemplo-publico")
    public String ejemploPublico() {
        return "dev/ejemplo-publico";
    }

    @GetMapping("/dev/ejemplo-admin")
    public String ejemploAdmin() {
        return "dev/ejemplo-admin";
    }
}
