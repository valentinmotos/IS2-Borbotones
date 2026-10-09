package com.example.cliente.controller;

import com.example.cliente.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/reportes")
public class ReporteController {
    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/personas-alquileres.pdf")
    public ResponseEntity<byte[]> personas() {
        try {
            return reporteService.personasPdf();
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), "No se pudo descargar el PDF", ex);
        }
    }

    @GetMapping("/libros-disponibles.xlsx")
    public ResponseEntity<byte[]> libros() {
        try {
            return reporteService.librosExcel();
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode(), "No se pudo descargar el Excel", ex);
        }
    }
}
