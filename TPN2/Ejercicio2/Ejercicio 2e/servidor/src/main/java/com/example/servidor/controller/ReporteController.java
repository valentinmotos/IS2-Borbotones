package com.example.servidor.controller;

import com.example.servidor.service.ReporteService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    public static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping(value = "/personas-alquileres.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> personas() {
        return descargar(reporteService.personasConAlquileresPdf(), "personas-alquileres.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping(value = "/libros-disponibles.xlsx", produces = EXCEL)
    public ResponseEntity<byte[]> libros() {
        return descargar(reporteService.librosDisponiblesExcel(), "libros-disponibles.xlsx", EXCEL);
    }

    private ResponseEntity<byte[]> descargar(byte[] datos, String nombre, String tipo) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(datos);
    }
}
