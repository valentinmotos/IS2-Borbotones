package com.dulcemayor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProveedoresAuthController {

    private final boolean google;

    public ProveedoresAuthController(@Value("${app.oauth.google.enabled:false}") boolean google) {
        this.google = google;
    }

    @GetMapping("/api/publico/auth/proveedores")
    public ProveedoresResponse proveedores() {
        return new ProveedoresResponse(google);
    }

    public record ProveedoresResponse(boolean google) {
    }
}
