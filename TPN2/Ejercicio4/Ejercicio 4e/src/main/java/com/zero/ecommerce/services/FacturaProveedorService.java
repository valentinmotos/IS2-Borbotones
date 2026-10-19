package com.zero.ecommerce.services;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.DetalleFacturaItemDTO;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaProveedorRepository;

/**
 * Compras de mercadería a proveedores (RF25). Una compra es una FacturaProveedor: se crea en SIN_DEFINIR, que
 * significa "pedida". Al recibirla pasa a PAGADA y se genera un movimiento de stock por cada detalle (RF26). Una
 * compra pedida también se puede anular.
 */
@Service
@Transactional(readOnly = true)
public class FacturaProveedorService {

    private final FacturaProveedorRepository repository;
    private final ProveedorService proveedorService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private final StockService stockService;

    public FacturaProveedorService(FacturaProveedorRepository repository, ProveedorService proveedorService,
            FormaDePagoService formaDePagoService, ProductoService productoService, StockService stockService) {
        this.repository = repository;
        this.proveedorService = proveedorService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
        this.stockService = stockService;
    }

    /**
     * Crea la compra pedida con el número siguiente y la fecha de hoy. La factura crea sus detalles (Creador) y
     * calcula su total (Experto).
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public FacturaProveedor crearFactura(String idProveedor, String idFormaDePago, List<DetalleFacturaItemDTO> detalles)
            throws ErrorServiceException {
        validar(idProveedor, idFormaDePago, detalles);
        FacturaProveedor factura = new FacturaProveedor();
        factura.setProveedor(proveedorService.buscarProveedor(idProveedor));
        factura.setFormaDePago(formaDePagoService.buscarFormaDePago(idFormaDePago));
        factura.setNumeroFactura(siguienteNumero());
        factura.setFechaFactura(LocalDate.now());
        factura.setEstado(EstadoFactura.SIN_DEFINIR);
        for (DetalleFacturaItemDTO item : sinFilasVacias(detalles)) {
            factura.agregarDetalle(productoService.buscarProducto(item.getProductoId()), item.getCantidad(),
                    item.getPrecioUnitario());
        }
        factura.setTotalPagado(factura.calcularTotal());
        return repository.save(factura);
    }

    public void validar(String idProveedor, String idFormaDePago, List<DetalleFacturaItemDTO> detalles)
            throws ErrorServiceException {
        if (idProveedor == null || idProveedor.isBlank()) {
            throw new ErrorServiceException("El proveedor es obligatorio.");
        }
        proveedorService.buscarProveedor(idProveedor);
        if (idFormaDePago == null || idFormaDePago.isBlank()) {
            throw new ErrorServiceException("La forma de pago es obligatoria.");
        }
        formaDePagoService.buscarFormaDePago(idFormaDePago);

        List<DetalleFacturaItemDTO> items = sinFilasVacias(detalles);
        if (items.isEmpty()) {
            throw new ErrorServiceException("La compra tiene que tener al menos un producto.");
        }
        Set<String> productos = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            DetalleFacturaItemDTO item = items.get(i);
            if (item.getProductoId() == null || item.getProductoId().isBlank()) {
                throw new ErrorServiceException("Elegí el producto del renglón " + (i + 1) + ".");
            }
            Producto producto = productoService.buscarProducto(item.getProductoId());
            String descripcion = describirProducto(producto);
            if (!productos.add(producto.getId())) {
                throw new ErrorServiceException("El producto " + descripcion
                        + " está repetido: cargalo en un solo renglón con la cantidad total.");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new ErrorServiceException("La cantidad de " + descripcion + " tiene que ser mayor a 0.");
            }
            Double precio = item.getPrecioUnitario();
            if (precio == null || !Double.isFinite(precio) || precio <= 0) {
                throw new ErrorServiceException("El precio de costo de " + descripcion + " tiene que ser mayor a 0.");
            }
        }
    }

    /**
     * Marca la compra como recibida (RF26): pasa a PAGADA y registra un movimiento de stock por cada detalle. Todo
     * corre en una transacción: si un movimiento falla, la compra sigue pedida y no queda stock a medias.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public FacturaProveedor recibirFactura(String id) throws ErrorServiceException {
        FacturaProveedor factura = buscarFactura(id);
        validarPedida(factura, "recibir");
        factura.setEstado(EstadoFactura.PAGADA);
        for (DetalleFactura detalle : factura.getDetalles()) {
            if (!detalle.isEliminado()) {
                stockService.registrarMovimiento(detalle);
            }
        }
        return repository.save(factura);
    }

    /** Anula una compra pedida. Como todavía no se recibió, no hay stock que revertir. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public FacturaProveedor anularFactura(String id) throws ErrorServiceException {
        FacturaProveedor factura = buscarFactura(id);
        validarPedida(factura, "anular");
        factura.setEstado(EstadoFactura.ANULADA);
        return repository.save(factura);
    }

    private void validarPedida(FacturaProveedor factura, String accion) throws ErrorServiceException {
        if (factura.getEstado() == EstadoFactura.PAGADA) {
            throw new ErrorServiceException("La compra N.º " + factura.getNumeroFactura()
                    + " ya fue recibida: no se puede " + (accion.equals("recibir") ? "recibir de nuevo" : accion) + ".");
        }
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ErrorServiceException("La compra N.º " + factura.getNumeroFactura()
                    + " está anulada: no se puede " + accion + ".");
        }
    }

    /** Baja lógica. Solo se elimina una compra pedida: una recibida ya generó movimientos de stock. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarFactura(String id) throws ErrorServiceException {
        FacturaProveedor factura = buscarFactura(id);
        if (factura.getEstado() != EstadoFactura.SIN_DEFINIR) {
            throw new ErrorServiceException("Solo se puede eliminar una compra que todavía no se recibió.");
        }
        factura.setEliminado(true);
        repository.save(factura);
    }

    public FacturaProveedor buscarFactura(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La compra no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La compra no existe o fue eliminada."));
    }

    /** Todas las compras, incluidas las eliminadas, de la más nueva a la más vieja. */
    public List<FacturaProveedor> listarFactura() {
        return repository.findAllByOrderByNumeroFacturaDesc();
    }

    public List<FacturaProveedor> listarFacturaActivo() {
        return repository.findByEliminadoFalseOrderByNumeroFacturaDesc();
    }

    public List<FacturaProveedor> listarFacturaPorEstado(EstadoFactura estado) {
        return repository.findByEstadoAndEliminadoFalseOrderByNumeroFacturaDesc(estado);
    }

    /** Compras activas filtradas por estado, proveedor y rango de fechas. Cada filtro es opcional (null o vacío). */
    public List<FacturaProveedor> listarFacturaActivo(EstadoFactura estado, String idProveedor, LocalDate desde,
            LocalDate hasta) throws ErrorServiceException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ErrorServiceException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        List<FacturaProveedor> compras = estado == null ? listarFacturaActivo() : listarFacturaPorEstado(estado);
        return compras.stream()
                .filter(f -> idProveedor == null || idProveedor.isBlank()
                        || f.getProveedor().getId().equals(idProveedor))
                .filter(f -> desde == null || !f.getFechaFactura().isBefore(desde))
                .filter(f -> hasta == null || !f.getFechaFactura().isAfter(hasta))
                .toList();
    }

    /** Estados para el filtro del listado: SIN_DEFINIR es "pedida" y PAGADA es "recibida". */
    public Map<String, String> listarEstadoCompra() {
        Map<String, String> estados = new LinkedHashMap<>();
        estados.put(EstadoFactura.SIN_DEFINIR.name(), "Pedida (sin definir)");
        estados.put(EstadoFactura.PAGADA.name(), "Recibida (pagada)");
        estados.put(EstadoFactura.ANULADA.name(), "Anulada");
        return estados;
    }

    /** Convierte el filtro de estado recibido al enum. Vacío o desconocido no filtra. */
    public EstadoFactura convertirEstado(String estado) {
        for (EstadoFactura valor : EstadoFactura.values()) {
            if (valor.name().equals(estado)) {
                return valor;
            }
        }
        return null;
    }

    /** Texto del pedido para mandarle al proveedor por WhatsApp: productos, cantidades y total. */
    public String armarMensajeWhatsApp(FacturaProveedor factura) {
        StringBuilder mensaje = new StringBuilder("Hola, ").append(factura.getProveedor().getRazonSocial())
                .append("\nLes enviamos el pedido de compra N.º ").append(factura.getNumeroFactura())
                .append(" de Zero:\n");
        for (DetalleFactura detalle : factura.getDetalles()) {
            if (detalle.isEliminado()) {
                continue;
            }
            Producto producto = detalle.getProducto();
            mensaje.append("- ").append(detalle.getCantidad()).append(" × ").append(producto.getNombre())
                    .append(" (talle ").append(producto.getTalle()).append(", código ").append(producto.getCodigo())
                    .append(") a ").append(formatearImporte(detalle.getPrecioUnitario())).append(" c/u\n");
        }
        mensaje.append("Total: ").append(formatearImporte(factura.getTotalPagado())).append("\n¡Muchas gracias!");
        return mensaje.toString();
    }

    // Numeración secuencial propia de las compras a proveedor, simulando la validada por ARCA.
    private long siguienteNumero() {
        return repository.findFirstByOrderByNumeroFacturaDesc()
                .map(f -> f.getNumeroFactura() + 1)
                .orElse(1L);
    }

    private String describirProducto(Producto producto) {
        return producto.getNombre() + " (talle " + producto.getTalle() + ")";
    }

    private String formatearImporte(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        formato.setMaximumFractionDigits(2);
        return "$" + formato.format(valor);
    }

    // Se ignoran los índices que deja Spring en null al quitar un renglón del medio y los renglones sin ningún dato.
    private List<DetalleFacturaItemDTO> sinFilasVacias(List<DetalleFacturaItemDTO> detalles) {
        if (detalles == null) {
            return List.of();
        }
        return detalles.stream()
                .filter(item -> item != null && !((item.getProductoId() == null || item.getProductoId().isBlank())
                        && item.getCantidad() == null && item.getPrecioUnitario() == null))
                .toList();
    }
}
