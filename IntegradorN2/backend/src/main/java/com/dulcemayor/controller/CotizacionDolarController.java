package com.dulcemayor.controller;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.service.CotizacionDolarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cotizaciones/dolar")
public class CotizacionDolarController {

    private final CotizacionDolarService cotizacionDolarService;

    public CotizacionDolarController(CotizacionDolarService cotizacionDolarService) {
        this.cotizacionDolarService = cotizacionDolarService;
    }

    @GetMapping("/vigente")
    public CotizacionDolarResponse buscarVigente() {
        return cotizacionDolarService.buscarVigente();
    }
}
