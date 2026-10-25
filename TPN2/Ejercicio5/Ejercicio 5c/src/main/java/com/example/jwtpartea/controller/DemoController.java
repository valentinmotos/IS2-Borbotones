package com.example.jwtpartea.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/publico")
    public ResponseEntity<Map<String, String>> publico() {
        return ResponseEntity.ok(Map.of("mensaje", "Endpoint publico"));
    }

    @GetMapping("/privado")
    public ResponseEntity<Map<String, String>> privado(Principal principal) {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Endpoint privado",
                "usuario", principal.getName()
        ));
    }
}
