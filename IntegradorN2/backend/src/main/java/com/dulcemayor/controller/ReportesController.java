package com.dulcemayor.controller;

import com.dulcemayor.dto.ReporteFiltrosResponse;
import com.dulcemayor.dto.ReporteVentasResponse;
import com.dulcemayor.service.ReportesExcelService;
import com.dulcemayor.service.ReportesPdfService;
import com.dulcemayor.service.ReportesService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** E5-03: reporte de ventas en pantalla, PDF y Excel con idéntico período y filtros. */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('JEFE', 'ADMINISTRATIVO')")
public class ReportesController {
    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportesService reportes;
    private final ReportesPdfService pdf;
    private final ReportesExcelService excel;

    public ReportesController(ReportesService reportes, ReportesPdfService pdf, ReportesExcelService excel) {
        this.reportes = reportes;
        this.pdf = pdf;
        this.excel = excel;
    }

    @GetMapping("/filtros")
    public ReporteFiltrosResponse filtros() { return reportes.listarFiltros(); }

    @GetMapping("/ventas")
    public ReporteVentasResponse ventas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String departamentoId,
            @RequestParam(required = false) String preventorId) {
        return reportes.buscarVentas(desde, hasta, departamentoId, preventorId);
    }

    @GetMapping(value = "/ventas/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String departamentoId,
            @RequestParam(required = false) String preventorId) {
        ReporteVentasResponse reporte = reportes.buscarVentas(desde, hasta, departamentoId, preventorId);
        String nombre = "reporte-ventas-%s-%s.pdf".formatted(desde, hasta);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(pdf.generar(reporte));
    }

    @GetMapping(value = "/ventas/excel", produces = EXCEL)
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String departamentoId,
            @RequestParam(required = false) String preventorId) {
        ReporteVentasResponse reporte = reportes.buscarVentas(desde, hasta, departamentoId, preventorId);
        String nombre = "reporte-ventas-%s-%s.xlsx".formatted(desde, hasta);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(EXCEL))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(excel.generar(reporte));
    }
}
