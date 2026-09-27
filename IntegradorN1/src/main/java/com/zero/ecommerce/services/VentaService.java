package com.zero.ecommerce.services;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleCompra;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;

/**
 * Ventas a clientes: la FacturaCliente creada desde la OrdenCompra. Contrato del kickoff de la Etapa 4:
 * confirmarCompra (E4-02), registrarPago y anularVenta (E4-03).
 */
@Service
@Transactional(readOnly = true)
public class VentaService {

    public static final String MENSAJE_PERFIL_INCOMPLETO =
            "Completá tu perfil (datos, dirección de entrega y teléfono) para poder confirmar la compra.";

    private final OrdenCompraRepository ordenCompraRepository;
    private final FacturaClienteRepository facturaClienteRepository;
    private final OrdenCompraService ordenCompraService;
    private final CarritoService carritoService;
    private final ClienteService clienteService;
    private final FormaDePagoService formaDePagoService;
    private final StockService stockService;
    private final VigenciaPrecioService vigenciaPrecioService;
    private final NotificacionCompraService notificacionCompraService;

    public VentaService(OrdenCompraRepository ordenCompraRepository, FacturaClienteRepository facturaClienteRepository,
            OrdenCompraService ordenCompraService, CarritoService carritoService, ClienteService clienteService,
            FormaDePagoService formaDePagoService, StockService stockService,
            VigenciaPrecioService vigenciaPrecioService, NotificacionCompraService notificacionCompraService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.facturaClienteRepository = facturaClienteRepository;
        this.ordenCompraService = ordenCompraService;
        this.carritoService = carritoService;
        this.clienteService = clienteService;
        this.formaDePagoService = formaDePagoService;
        this.stockService = stockService;
        this.vigenciaPrecioService = vigenciaPrecioService;
        this.notificacionCompraService = notificacionCompraService;
    }

    /**
     * Confirma el carrito del cliente (RF18): revalida stock y precio de cada ítem, pasa la orden a PENDIENTE_PAGO y
     * crea la FacturaCliente en SIN_DEFINIR con el número siguiente y un detalle por ítem con el precio vigente
     * (Creador). Después manda el correo de confirmación. El carrito queda vacío porque la orden ya no está en
     * PENDIENTE_COMPLETAR: la próxima vez que el cliente agregue algo se crea una orden nueva.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public FacturaCliente confirmarCompra(String idCliente, String idFormaPago) throws ErrorServiceException {
        Cliente cliente = clienteService.buscarCliente(idCliente);
        FormaDePago formaDePago = validar(cliente, idFormaPago);
        OrdenCompra orden = carritoService.obtenerCarrito(idCliente);
        revalidarItems(orden);

        orden.confirmar();
        orden.setFecha(LocalDate.now());
        ordenCompraRepository.save(orden);

        FacturaCliente factura = new FacturaCliente();
        factura.setNumeroFactura(siguienteNumero());
        factura.setFechaFactura(LocalDate.now());
        factura.setEstado(EstadoFactura.SIN_DEFINIR);
        factura.setCliente(cliente);
        factura.setOrdenCompra(orden);
        factura.setFormaDePago(formaDePago);
        for (DetalleCompra detalle : orden.getDetalles()) {
            if (!detalle.isEliminado()) {
                factura.agregarDetalle(detalle.getProducto(), detalle.getCantidad(), detalle.getPrecioUnitario());
            }
        }
        factura.setTotalPagado(factura.calcularTotal());
        facturaClienteRepository.save(factura);

        notificacionCompraService.enviarConfirmacion(orden);
        return factura;
    }

    /** El cliente necesita el perfil completo para comprar, y la forma de pago tiene que estar activa. */
    public FormaDePago validar(Cliente cliente, String idFormaPago) throws ErrorServiceException {
        if (cliente.getUsuario() == null || !clienteService.perfilCompleto(cliente.getUsuario().getId())) {
            throw new ErrorServiceException(MENSAJE_PERFIL_INCOMPLETO);
        }
        if (idFormaPago == null || idFormaPago.isBlank()) {
            throw new ErrorServiceException("Elegí una forma de pago.");
        }
        return formaDePagoService.buscarFormaDePago(idFormaPago);
    }

    /**
     * Registra el pago de una venta y descuenta sus productos del stock. La factura pagada vuelve idempotente la
     * operación: una segunda notificación del mismo pago no cambia estados, stock ni envía otro correo.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void registrarPago(String idOrden) throws ErrorServiceException {
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        FacturaCliente factura = buscarFactura(idOrden);
        if (factura.getEstado() == EstadoFactura.PAGADA) {
            return;
        }
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ErrorServiceException("No se puede registrar el pago de una factura anulada.");
        }

        orden.registrarPago();
        for (var detalle : factura.getDetalles()) {
            if (!detalle.isEliminado()) {
                stockService.registrarMovimiento(detalle);
            }
        }
        factura.setEstado(EstadoFactura.PAGADA);
        factura.setTotalPagado(factura.calcularTotal());
        ordenCompraRepository.save(orden);
        facturaClienteRepository.save(factura);
        notificacionCompraService.notificarCambioEstado(orden);
    }

    /**
     * Anula la venta (RF19): la orden y su factura pasan a ANULADA. Quién puede anular y en qué estado lo decide la
     * orden con puedeAnularse(esAdmin).
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void anularVenta(String idOrden, boolean esAdmin) throws ErrorServiceException {
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(idOrden).orElse(null);
        boolean estabaPagada = factura != null && factura.getEstado() == EstadoFactura.PAGADA;
        // anular verifica con puedeAnularse y, si no corresponde, lanza la excepción con el mensaje del estado.
        orden.anular(esAdmin);
        if (factura != null) {
            if (estabaPagada) {
                for (var detalle : factura.getDetalles()) {
                    if (!detalle.isEliminado()) {
                        stockService.revertirMovimiento(detalle);
                    }
                }
            }
            factura.setEstado(EstadoFactura.ANULADA);
            facturaClienteRepository.save(factura);
        }
        ordenCompraRepository.save(orden);
        notificacionCompraService.notificarCambioEstado(orden);
    }

    private FacturaCliente buscarFactura(String idOrden) throws ErrorServiceException {
        return ordenCompraService.buscarFacturaDePedido(idOrden)
                .orElseThrow(() -> new ErrorServiceException("El pedido no tiene una factura asociada."));
    }

    /**
     * Cada ítem tiene que seguir activo, con precio vigente y con stock suficiente. Si el precio cambió desde que se
     * agregó, se toma el vigente, así la orden y la factura quedan con el precio del momento de la compra.
     */
    private void revalidarItems(OrdenCompra orden) throws ErrorServiceException {
        for (DetalleCompra detalle : orden.getDetalles()) {
            if (detalle.isEliminado()) {
                continue;
            }
            Producto producto = detalle.getProducto();
            if (producto == null || producto.isEliminado()) {
                throw new ErrorServiceException(
                        "Un producto de tu carrito ya no está disponible. Revisá tu carrito antes de confirmar.");
            }
            double precioVigente;
            try {
                precioVigente = vigenciaPrecioService.buscarPrecioVigente(producto.getId());
            } catch (ErrorServiceException e) {
                throw new ErrorServiceException("El producto \"" + producto.getNombre()
                        + "\" no tiene precio vigente. Revisá tu carrito antes de confirmar.");
            }
            int stockActual = stockService.buscarStockActual(producto.getId());
            if (detalle.getCantidad() > stockActual) {
                throw new ErrorServiceException("No hay stock suficiente de \"" + producto.getNombre() + "\" (talle "
                        + producto.getTalle() + "). Stock disponible: " + stockActual
                        + ". Revisá tu carrito antes de confirmar.");
            }
            detalle.calcularSubtotal(precioVigente);
        }
        orden.recalcularTotal();
    }

    // Numeración secuencial propia de las ventas, simulando la validada por ARCA.
    private long siguienteNumero() {
        return facturaClienteRepository.findFirstByOrderByNumeroFacturaDesc()
                .map(f -> f.getNumeroFactura() + 1)
                .orElse(1L);
    }
}
