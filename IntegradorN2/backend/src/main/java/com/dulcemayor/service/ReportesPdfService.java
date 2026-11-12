package com.dulcemayor.service;

import com.dulcemayor.dto.ReporteVentaDetalleResponse;
import com.dulcemayor.dto.ReporteVentasResponse;
import com.dulcemayor.exception.ErrorServiceException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/** Genera en memoria el PDF con el mismo detalle y total expuestos en pantalla. */
@Service
public class ReportesPdfService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MARCA = new Color(0x8B, 0x1E, 0x3F);
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, MARCA);
    private static final Font TEXTO = new Font(Font.HELVETICA, 9);
    private static final Font CHICA = new Font(Font.HELVETICA, 7);
    private static final Font CABECERA = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);

    public byte[] generar(ReporteVentasResponse reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Reporte de ventas");
            documento.addAuthor("Dulce Mayor");
            documento.open();
            Paragraph titulo = new Paragraph("Dulce Mayor · Reporte de ventas", TITULO);
            titulo.setSpacingAfter(8);
            documento.add(titulo);
            Paragraph periodo = new Paragraph("Período: " + FECHA.format(reporte.desde()) + " al "
                    + FECHA.format(reporte.hasta()), TEXTO);
            periodo.setSpacingAfter(12);
            documento.add(periodo);

            PdfPTable tabla = new PdfPTable(new float[] {
                    0.8f, 0.8f, 0.8f, 1.5f, 1.3f, 1.2f, 1.0f, 1.0f, 0.9f });
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String encabezado : new String[] { "Factura", "Fecha", "Pedido", "Cliente", "Comercio",
                    "Departamento", "Preventor", "Estado de envío", "Monto" }) {
                celda(tabla, encabezado, CABECERA, MARCA, Element.ALIGN_LEFT);
            }
            for (ReporteVentaDetalleResponse venta : reporte.detalle()) {
                String[] valores = { numeroVisible(venta.numeroFactura()), FECHA.format(venta.fechaFactura()),
                        numeroVisible(venta.numeroPedido()), venta.cliente(), venta.puntoDeVenta(),
                        venta.departamento(), venta.preventor(), venta.estadoEnvio().name(),
                        String.format("$ %,.2f", venta.monto()) };
                for (String valor : valores) celda(tabla, valor, CHICA, Color.WHITE, Element.ALIGN_LEFT);
            }
            if (reporte.detalle().isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("No hay ventas facturadas para el período y los filtros elegidos.", TEXTO));
                vacio.setColspan(9);
                vacio.setPadding(8);
                tabla.addCell(vacio);
            }
            documento.add(tabla);
            Paragraph total = new Paragraph("Ventas: " + reporte.cantidadVentas()
                    + "     Total facturado: " + String.format("$ %,.2f", reporte.montoTotal()),
                    new Font(Font.HELVETICA, 11, Font.BOLD, MARCA));
            total.setSpacingBefore(12);
            documento.add(total);
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException ex) {
            if (documento.isOpen()) documento.close();
            throw new ErrorServiceException("No se pudo generar el PDF del reporte.");
        }
    }

    private static void celda(PdfPTable tabla, String texto, Font fuente, Color fondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private static String numeroVisible(long numero) { return "%08d".formatted(numero); }
}
