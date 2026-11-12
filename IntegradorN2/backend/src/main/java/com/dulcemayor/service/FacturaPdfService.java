package com.dulcemayor.service;

import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.Mayorista;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Persona;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.MayoristaRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4-04: factura electrónica en PDF con OpenPDF, armada en memoria cada vez que se pide (no se guarda en
 * disco ni en la base). Toma la idea de la factura "de papel" de TP2 Ej. 4e ({@code FacturaReportService}:
 * encabezado de la empresa, cliente, renglones y totales) y el armado en memoria de TP2 Ej. 2d.
 *
 * NOTA (E4-04 → E4-07 / E4-08): {@link #generarFacturaPdf(String)} no controla quién la pide. El control de
 * acceso lo hace quien la expone: hoy {@code /api/facturas/{id}/pdf} solo para JEFE y ADMINISTRATIVO; E4-08
 * debe verificar que la factura sea de un punto de venta del minorista logueado antes de llamarlo, y E4-07
 * lo usa para adjuntar el PDF al correo.
 */
@Service
@Transactional(readOnly = true)
public class FacturaPdfService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MARCA = new Color(0x8B, 0x1E, 0x3F);
    private static final Color GRIS = new Color(0xF2, 0xF2, 0xF2);
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, MARCA);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font CABECERA = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CHICA = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY);

    private final FacturaRepository facturas;
    private final MayoristaRepository mayoristas;

    public FacturaPdfService(FacturaRepository facturas, MayoristaRepository mayoristas) {
        this.facturas = facturas;
        this.mayoristas = mayoristas;
    }

    /** Nombre del archivo descargado, por ejemplo "factura-00000012.pdf". */
    public String nombreArchivo(String idFactura) {
        return "factura-" + FacturacionService.numeroVisible(buscar(idFactura).getNumeroFactura()) + ".pdf";
    }

    /** Bytes del PDF de la factura. 404 si la factura no existe o está dada de baja. */
    public byte[] generarFacturaPdf(String idFactura) {
        Factura factura = buscar(idFactura);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Factura " + FacturacionService.numeroVisible(factura.getNumeroFactura()));
            documento.addAuthor("Dulce Mayor");
            documento.open();
            encabezado(documento, factura);
            cliente(documento, factura.getPedido());
            detalle(documento, factura);
            totales(documento, factura);
            documento.add(new Paragraph("Comprobante generado por el sistema de Dulce Mayor.", CHICA));
            documento.close();
        } catch (DocumentException ex) {
            throw new ErrorServiceException("No se pudo generar la factura en PDF.");
        }
        return salida.toByteArray();
    }

    private Factura buscar(String idFactura) {
        if (idFactura == null || idFactura.isBlank()) {
            throw new RecursoNoEncontradoException("No se encontró la factura.");
        }
        return facturas.findByIdAndEliminadoFalse(idFactura)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la factura."));
    }

    private void encabezado(Document documento, Factura factura) throws DocumentException {
        Mayorista empresa = mayoristas.findByEliminadoFalse().stream().findFirst().orElse(null);
        PdfPTable tabla = new PdfPTable(new float[] {3, 2});
        tabla.setWidthPercentage(100);

        PdfPCell emisor = celdaSinBorde();
        emisor.addElement(new Paragraph(empresa == null ? "Dulce Mayor" : empresa.getRazonSocial(), TITULO));
        emisor.addElement(new Paragraph("CUIT: " + (empresa == null ? "-" : empresa.getCuit()), NORMAL));
        emisor.addElement(new Paragraph("Distribuidora mayorista de golosinas", NORMAL));
        tabla.addCell(emisor);

        PdfPCell comprobante = celdaSinBorde();
        comprobante.setHorizontalAlignment(Element.ALIGN_RIGHT);
        comprobante.addElement(derecha("FACTURA", TITULO));
        comprobante.addElement(derecha("N° " + FacturacionService.numeroVisible(factura.getNumeroFactura()), NEGRITA));
        comprobante.addElement(derecha("Fecha: " + factura.getFechaFactura().format(FECHA), NORMAL));
        comprobante.addElement(derecha("Estado: " + estado(factura.getEstado()), NORMAL));
        tabla.addCell(comprobante);
        documento.add(tabla);
        documento.add(separador());
    }

    private void cliente(Document documento, Pedido pedido) throws DocumentException {
        PuntoDeVenta punto = pedido.getPuntoDeVenta();
        Minorista minorista = punto.getMinorista();
        PdfPTable tabla = new PdfPTable(new float[] {1, 1});
        tabla.setWidthPercentage(100);

        PdfPCell datosCliente = celdaSinBorde();
        datosCliente.addElement(new Paragraph("Cliente", NEGRITA));
        datosCliente.addElement(new Paragraph("Comercio: " + punto.getNombreComercio(), NORMAL));
        datosCliente.addElement(new Paragraph("Responsable: " + nombre(minorista), NORMAL));
        datosCliente.addElement(new Paragraph("Documento: " + documento(minorista), NORMAL));
        datosCliente.addElement(new Paragraph("Dirección: " + direccion(punto.getDireccion()), NORMAL));
        tabla.addCell(datosCliente);

        PdfPCell datosPedido = celdaSinBorde();
        datosPedido.addElement(new Paragraph("Pedido", NEGRITA));
        datosPedido.addElement(new Paragraph("N° de pedido: " + pedido.getNumeroPedido(), NORMAL));
        datosPedido.addElement(new Paragraph("Cargado: " + pedido.getFechaHoraCarga().toLocalDate().format(FECHA), NORMAL));
        datosPedido.addElement(new Paragraph("Preventor: " + nombre(pedido.getPreventor()), NORMAL));
        tabla.addCell(datosPedido);
        documento.add(tabla);
        documento.add(separador());
    }

    private void detalle(Document documento, Factura factura) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[] {1.3f, 3.2f, 1.8f, 1, 1.5f, 1.6f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : new String[] {"Código", "Producto", "Presentación", "Cant.", "P. unitario", "Subtotal"}) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, CABECERA));
            celda.setBackgroundColor(MARCA);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
        boolean par = false;
        for (DetalleFactura linea : factura.getDetalles()) {
            Color fondo = par ? GRIS : Color.WHITE;
            tabla.addCell(celda(linea.getProducto().getCodigo(), Element.ALIGN_LEFT, fondo));
            tabla.addCell(celda(linea.getProducto().getNombre(), Element.ALIGN_LEFT, fondo));
            tabla.addCell(celda(linea.getProducto().getPresentacion(), Element.ALIGN_LEFT, fondo));
            tabla.addCell(celda(String.valueOf(linea.getCantidad()), Element.ALIGN_RIGHT, fondo));
            tabla.addCell(celda(pesos(linea.getPrecioUnitario()), Element.ALIGN_RIGHT, fondo));
            tabla.addCell(celda(pesos(linea.getSubtotal()), Element.ALIGN_RIGHT, fondo));
            par = !par;
        }
        documento.add(tabla);
    }

    private void totales(Document documento, Factura factura) throws DocumentException {
        Pedido pedido = factura.getPedido();
        PdfPTable tabla = new PdfPTable(new float[] {3, 1.6f});
        tabla.setWidthPercentage(45);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(8);
        tabla.setSpacingAfter(16);
        filaTotal(tabla, "Subtotal", pesos(pedido.getSubtotal()), NORMAL);
        String promocion = pedido.getCodigoPromocion() == null ? "Descuento" : "Descuento (" + pedido.getCodigoPromocion() + ")";
        filaTotal(tabla, promocion, "- " + pesos(pedido.getDescuento()), NORMAL);
        filaTotal(tabla, "TOTAL", pesos(factura.getTotal()), NEGRITA);
        documento.add(tabla);
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        PdfPCell izquierda = new PdfPCell(new Phrase(etiqueta, fuente));
        izquierda.setBorder(Rectangle.TOP);
        izquierda.setPadding(4);
        PdfPCell derecha = new PdfPCell(new Phrase(valor, fuente));
        derecha.setBorder(Rectangle.TOP);
        derecha.setPadding(4);
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(izquierda);
        tabla.addCell(derecha);
    }

    private static PdfPCell celda(String texto, int alineacion, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "-" : texto, NORMAL));
        celda.setHorizontalAlignment(alineacion);
        celda.setBackgroundColor(fondo);
        celda.setPadding(4);
        return celda;
    }

    private static PdfPCell celdaSinBorde() {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    private static Paragraph derecha(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_RIGHT);
        return parrafo;
    }

    private static Paragraph separador() {
        Paragraph espacio = new Paragraph(" ", NORMAL);
        espacio.setSpacingAfter(4);
        return espacio;
    }

    /** Importe con formato argentino: "$ 1.234,50". */
    static String pesos(double importe) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));
        return "$ " + formato.format(importe);
    }

    private static String estado(EstadoFactura estado) {
        return switch (estado) {
            case PENDIENTE_PAGO -> "Pendiente de pago";
            case PAGADA -> "Pagada";
            case ANULADA -> "Anulada";
        };
    }

    private static String nombre(Persona persona) {
        return persona == null ? "-" : persona.getNombre() + " " + persona.getApellido();
    }

    private static String documento(Persona persona) {
        return persona == null || persona.getNumeroDocumento() == null ? "-"
                : persona.getTipoDocumento() + " " + persona.getNumeroDocumento();
    }

    private static String direccion(Direccion direccion) {
        if (direccion == null) {
            return "-";
        }
        StringBuilder texto = new StringBuilder(direccion.getCalle());
        if (direccion.getNumeracion() != null) {
            texto.append(' ').append(direccion.getNumeracion());
        }
        if (direccion.getLocalidad() != null) {
            texto.append(", ").append(direccion.getLocalidad().getNombre());
        }
        return texto.toString();
    }
}
