package com.dulcemayor.controller;

import com.dulcemayor.dto.PagoRequest;
import com.dulcemayor.dto.PagoResponse;
import com.dulcemayor.service.PagoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
public class PagoController {
    private final PagoService pagos;

    public PagoController(PagoService pagos) { this.pagos = pagos; }

    @PostMapping
    public PagoResponse registrar(@Valid @RequestBody PagoRequest request) { return pagos.registrar(request); }

    @GetMapping("/factura/{facturaId}")
    public List<PagoResponse> listarPorFactura(@PathVariable String facturaId) {
        return pagos.listarPorFactura(facturaId);
    }
}
