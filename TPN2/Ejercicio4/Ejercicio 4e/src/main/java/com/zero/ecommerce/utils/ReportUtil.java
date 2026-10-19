package com.zero.ecommerce.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * Genera reportes PDF con JasperReports a partir de un .jasper compilado en resources/reports.
 * Sigue los tres pasos de la guía "Reportes": cargar el reporte, llenarlo con datos y exportarlo a PDF.
 */
public final class ReportUtil {

    private static final String CARPETA = "/reports/";

    private ReportUtil() {
    }

    /**
     * @param reporte    nombre del archivo sin extensión (ej: "factura" para reports/factura.jasper)
     * @param parametros valores de los $P{...} del reporte
     * @param datos      filas de la banda de detalle; cada propiedad del bean es un $F{...}
     */
    public static byte[] generarPdf(String reporte, Map<String, Object> parametros, Collection<?> datos)
            throws JRException, IOException {
        try (InputStream archivo = ReportUtil.class.getResourceAsStream(CARPETA + reporte + ".jasper")) {
            if (archivo == null) {
                throw new JRException("No se encontró el reporte " + reporte + ".jasper");
            }
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(archivo);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
                    parametros == null ? new HashMap<>() : parametros, new JRBeanCollectionDataSource(datos));
            return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }

    /** Respuesta HTTP que el navegador descarga como archivo PDF. */
    public static ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
