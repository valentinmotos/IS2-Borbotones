package com.dulcemayor.service;

import com.dulcemayor.dto.ReporteVentaAgrupadaResponse;
import com.dulcemayor.dto.ReporteVentaDetalleResponse;
import com.dulcemayor.dto.ReporteVentasResponse;
import com.dulcemayor.exception.ErrorServiceException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Exporta resumen y detalle del mismo resultado de ventas que alimenta pantalla y PDF. */
@Service
public class ReportesExcelService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String FORMATO_MONEDA = "$ #,##0.00";

    public byte[] generar(ReporteVentasResponse reporte) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle cabecera = estiloCabecera(libro);
            CellStyle total = estiloTotal(libro);
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat(FORMATO_MONEDA));

            escribirResumen(libro, reporte, cabecera, total, moneda);
            escribirDetalle(libro, reporte, cabecera, moneda);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new ErrorServiceException("No se pudo generar el Excel del reporte.");
        }
    }

    private static void escribirResumen(Workbook libro, ReporteVentasResponse reporte,
            CellStyle cabecera, CellStyle total, CellStyle moneda) {
        Sheet hoja = libro.createSheet("Ventas por departamento");
        crearCelda(hoja.createRow(0), 0, "Reporte histórico de ventas", null);
        crearCelda(hoja.createRow(1), 0,
                "Período: " + FECHA.format(reporte.desde()) + " al " + FECHA.format(reporte.hasta()), null);
        Row encabezados = hoja.createRow(3);
        String[] titulos = { "Departamento", "Preventor", "Cantidad de ventas", "Total facturado" };
        for (int i = 0; i < titulos.length; i++) crearCelda(encabezados, i, titulos[i], cabecera);
        int indice = 4;
        for (ReporteVentaAgrupadaResponse grupo : reporte.resumen()) {
            Row fila = hoja.createRow(indice++);
            crearCelda(fila, 0, grupo.departamento(), null);
            crearCelda(fila, 1, grupo.preventor(), null);
            crearCelda(fila, 2, grupo.cantidadVentas(), null);
            crearCelda(fila, 3, grupo.montoTotal().doubleValue(), moneda);
        }
        Row totalFila = hoja.createRow(indice);
        crearCelda(totalFila, 0, "TOTAL DEL PERÍODO", total);
        crearCelda(totalFila, 1, "", total);
        crearCelda(totalFila, 2, reporte.cantidadVentas(), total);
        crearCelda(totalFila, 3, reporte.montoTotal().doubleValue(), total);
        totalFila.getCell(3).setCellStyle(estiloTotalMoneda(libro, total));
        hoja.setColumnWidth(0, 34 * 256);
        hoja.setColumnWidth(1, 30 * 256);
        hoja.setColumnWidth(2, 22 * 256);
        hoja.setColumnWidth(3, 20 * 256);
        hoja.createFreezePane(0, 4);
        hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3, Math.max(3, indice - 1), 0, 3));
    }

    private static void escribirDetalle(Workbook libro, ReporteVentasResponse reporte,
            CellStyle cabecera, CellStyle moneda) {
        Sheet hoja = libro.createSheet("Detalle de ventas");
        crearCelda(hoja.createRow(0), 0,
                "Período: " + FECHA.format(reporte.desde()) + " al " + FECHA.format(reporte.hasta()), null);
        Row encabezados = hoja.createRow(2);
        String[] titulos = { "N.º factura", "Fecha factura", "N.º pedido", "Fecha del pedido", "Cliente",
                "Comercio", "Departamento", "Preventor", "Estado de envío", "Monto facturado" };
        for (int i = 0; i < titulos.length; i++) crearCelda(encabezados, i, titulos[i], cabecera);
        int indice = 3;
        for (ReporteVentaDetalleResponse venta : reporte.detalle()) {
            Row fila = hoja.createRow(indice++);
            crearCelda(fila, 0, numeroVisible(venta.numeroFactura()), null);
            crearCelda(fila, 1, FECHA.format(venta.fechaFactura()), null);
            crearCelda(fila, 2, numeroVisible(venta.numeroPedido()), null);
            crearCelda(fila, 3, FECHA_HORA.format(venta.fechaPedido()), null);
            crearCelda(fila, 4, venta.cliente(), null);
            crearCelda(fila, 5, venta.puntoDeVenta(), null);
            crearCelda(fila, 6, venta.departamento(), null);
            crearCelda(fila, 7, venta.preventor(), null);
            crearCelda(fila, 8, venta.estadoEnvio().name(), null);
            crearCelda(fila, 9, venta.monto().doubleValue(), moneda);
        }
        Row totalFila = hoja.createRow(indice);
        crearCelda(totalFila, 0, "TOTAL FACTURADO", cabecera);
        totalFila.getCell(0).setCellStyle(cabecera);
        crearCelda(totalFila, 9, reporte.montoTotal().doubleValue(), moneda);
        hoja.setColumnWidth(0, 15 * 256);
        hoja.setColumnWidth(1, 17 * 256);
        hoja.setColumnWidth(2, 15 * 256);
        hoja.setColumnWidth(3, 20 * 256);
        hoja.setColumnWidth(4, 30 * 256);
        hoja.setColumnWidth(5, 30 * 256);
        hoja.setColumnWidth(6, 25 * 256);
        hoja.setColumnWidth(7, 25 * 256);
        hoja.setColumnWidth(8, 22 * 256);
        hoja.setColumnWidth(9, 20 * 256);
        hoja.createFreezePane(0, 3);
        hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(2, indice - 1), 0, 9));
    }

    private static CellStyle estiloCabecera(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_RED.getIndex());
        estilo.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private static CellStyle estiloTotal(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex());
        estilo.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private static CellStyle estiloTotalMoneda(Workbook libro, CellStyle base) {
        CellStyle estilo = libro.createCellStyle();
        estilo.cloneStyleFrom(base);
        estilo.setDataFormat(libro.createDataFormat().getFormat(FORMATO_MONEDA));
        return estilo;
    }

    private static void crearCelda(Row fila, int indice, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(indice);
        celda.setCellValue(valor == null ? "" : valor);
        if (estilo != null) celda.setCellStyle(estilo);
    }

    private static void crearCelda(Row fila, int indice, long valor, CellStyle estilo) {
        Cell celda = fila.createCell(indice);
        celda.setCellValue(valor);
        if (estilo != null) celda.setCellStyle(estilo);
    }

    private static void crearCelda(Row fila, int indice, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(indice);
        celda.setCellValue(valor);
        if (estilo != null) celda.setCellStyle(estilo);
    }

    private static String numeroVisible(long numero) { return "%08d".formatted(numero); }
}
