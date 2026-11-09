package com.dulcemayor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.service.CotizacionDolarService;

/**
 * Cotización vigente del dólar (E1-08). Pública: la muestra el sitio (E3-02, E3-03) sin login,
 * por eso va bajo /api/publico (abierto en SecurityConfig).
 */
@RestController
@RequestMapping("/api/publico/cotizacion")
public class CotizacionDolarController {

    private final CotizacionDolarService cotizacionDolarService;

    public CotizacionDolarController(CotizacionDolarService cotizacionDolarService) {
        this.cotizacionDolarService = cotizacionDolarService;
    }

    @GetMapping
    public CotizacionDolarResponse buscarVigente() {
        return cotizacionDolarService.buscarCotizacionVigente();
    }
}
