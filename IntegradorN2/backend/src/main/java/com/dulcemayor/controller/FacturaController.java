package com.dulcemayor.controller;

import com.dulcemayor.dto.FacturaResponse;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.service.FacturaPdfService;
import com.dulcemayor.service.FacturacionService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E4-04: consulta y descarga de facturas para gestión (Jefe y Administrativo).
 *
 * NOTA (E4-04 → E4-08): el minorista NO usa estas rutas. Sus facturas van bajo {@code /api/minorista/**},
 * resolviendo la identidad desde el token y validando que la factura sea de uno de sus puntos de venta
 * antes de llamar a {@code FacturaPdfService.generarFacturaPdf(id)}.
 */
@RestController
@RequestMapping("/api/facturas")
@PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
public class FacturaController {
    private final FacturacionService facturacion;
    private final FacturaPdfService pdf;

    public FacturaController(FacturacionService facturacion, FacturaPdfService pdf) {
        this.facturacion = facturacion;
        this.pdf = pdf;
    }

    @GetMapping
    public List<FacturaResponse> listar(@RequestParam(required = false) EstadoFactura estado) {
        return estado == null ? facturacion.listarFactura() : facturacion.listarFacturaPorEstado(estado);
    }

    @GetMapping("/{id}")
    public FacturaResponse buscar(@PathVariable String id) {
        return facturacion.buscarFactura(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargar(@PathVariable String id) {
        byte[] contenido = pdf.generarFacturaPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pdf.nombreArchivo(id)).build().toString())
                .body(contenido);
    }
}
