package com.zero.ecommerce.services;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.sf.jasperreports.engine.JRParameter;

import com.zero.ecommerce.dto.RenglonFacturaDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.utils.ReportUtil;

/**
 * Factura de venta en PDF (TP2, ejercicio 4e). Arma los parámetros del encabezado y los renglones de la factura y
 * se los pasa a {@link ReportUtil} con el reporte reports/factura.jasper.
 */
@Service
@Transactional(readOnly = true)
public class FacturaReportService {

    private static final String REPORTE = "factura";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdenCompraService ordenCompraService;
    private final EmpresaService empresaService;

    public FacturaReportService(OrdenCompraService ordenCompraService, EmpresaService empresaService) {
        this.ordenCompraService = ordenCompraService;
        this.empresaService = empresaService;
    }

    /** Nombre del archivo descargado, por ejemplo "factura-00000012.pdf". */
    public String nombreArchivo(String idOrden) throws ErrorServiceException {
        return "factura-" + numero(buscarFactura(idOrden)) + ".pdf";
    }

    /** El PDF de la factura del pedido. Falla si el pedido todavía no tiene factura. */
    public byte[] generarFactura(String idOrden) throws ErrorServiceException {
        FacturaCliente factura = buscarFactura(idOrden);
        try {
            return ReportUtil.generarPdf(REPORTE, parametros(factura), renglones(factura));
        } catch (Exception e) {
            throw new ErrorServiceException("No se pudo generar la factura en PDF.");
        }
    }

    private FacturaCliente buscarFactura(String idOrden) throws ErrorServiceException {
        return ordenCompraService.buscarFacturaDePedido(idOrden)
                .orElseThrow(() -> new ErrorServiceException("El pedido todavía no tiene factura."));
    }

    private Map<String, Object> parametros(FacturaCliente factura) throws ErrorServiceException {
        Empresa empresa = empresaService.buscarSedeCentral();
        Cliente cliente = factura.getCliente();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("EMPRESA_NOMBRE", empresa.getRazonSocial());
        parametros.put("EMPRESA_CUIT", texto(empresa.getCuit()));
        parametros.put("EMPRESA_DIRECCION", direccion(empresa.getDireccion()));
        parametros.put("NUMERO", numero(factura));
        parametros.put("FECHA", factura.getFechaFactura() == null ? "-" : factura.getFechaFactura().format(FECHA));
        parametros.put("PEDIDO", factura.getOrdenCompra() == null ? "-" : factura.getOrdenCompra().getIdentificadorCompra());
        parametros.put("ESTADO", estado(factura.getEstado()));
        parametros.put("CLIENTE_NOMBRE", cliente == null ? "-" : cliente.getNombre() + " " + cliente.getApellido());
        parametros.put("CLIENTE_DOCUMENTO", cliente == null || cliente.getNumeroDocumento() == null ? "-"
                : cliente.getTipoDocumento() + " " + cliente.getNumeroDocumento());
        parametros.put("CLIENTE_DIRECCION", cliente == null ? "-" : direccion(cliente.getDireccion()));
        parametros.put("CLIENTE_EMAIL", cliente == null || cliente.getUsuario() == null ? "-"
                : cliente.getUsuario().getNombreUsuario());
        parametros.put("FORMA_PAGO", factura.getFormaDePago() == null ? "-" : factura.getFormaDePago().getObservacion());
        parametros.put("TOTAL", factura.calcularTotal());
        parametros.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-AR"));
        return parametros;
    }

    private List<RenglonFacturaDTO> renglones(FacturaCliente factura) {
        return factura.getDetalles().stream()
                .filter(d -> !d.isEliminado())
                .map(d -> new RenglonFacturaDTO(texto(d.getProducto().getCodigo()), d.getProducto().getNombre(),
                        texto(d.getProducto().getTalle()), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()))
                .toList();
    }

    // SIN_DEFINIR es la factura de un pedido que todavía no se pagó.
    private String estado(EstadoFactura estado) {
        if (estado == null) {
            return "-";
        }
        return estado == EstadoFactura.SIN_DEFINIR ? "PENDIENTE DE PAGO" : estado.name();
    }

    private String numero(FacturaCliente factura) {
        return String.format("%08d", factura.getNumeroFactura());
    }

    private String direccion(Direccion direccion) {
        return direccion == null ? "-" : direccion.describir();
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }
}
