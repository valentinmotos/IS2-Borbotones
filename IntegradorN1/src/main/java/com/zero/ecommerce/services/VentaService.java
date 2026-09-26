package com.zero.ecommerce.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
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

    private final OrdenCompraRepository ordenCompraRepository;
    private final FacturaClienteRepository facturaClienteRepository;
    private final OrdenCompraService ordenCompraService;
    private final StockService stockService;
    private final NotificacionCompraService notificacionCompraService;

    public VentaService(OrdenCompraRepository ordenCompraRepository, FacturaClienteRepository facturaClienteRepository,
            OrdenCompraService ordenCompraService, StockService stockService,
            NotificacionCompraService notificacionCompraService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.facturaClienteRepository = facturaClienteRepository;
        this.ordenCompraService = ordenCompraService;
        this.stockService = stockService;
        this.notificacionCompraService = notificacionCompraService;
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
}
