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

    public VentaService(OrdenCompraRepository ordenCompraRepository, FacturaClienteRepository facturaClienteRepository,
            OrdenCompraService ordenCompraService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.facturaClienteRepository = facturaClienteRepository;
        this.ordenCompraService = ordenCompraService;
    }

    /**
     * Anula la venta (RF19): la orden y su factura pasan a ANULADA. Quién puede anular y en qué estado lo decide la
     * orden con puedeAnularse(esAdmin).
     */
    // TODO E4-03: implementación mínima para la anulación del cliente (E4-04), que solo anula antes del pago. Falta
    // reingresar el stock con StockService.revertirMovimiento cuando la factura ya estaba PAGADA (anulación del admin).
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void anularVenta(String idOrden, boolean esAdmin) throws ErrorServiceException {
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        // anular verifica con puedeAnularse y, si no corresponde, lanza la excepción con el mensaje del estado.
        orden.anular(esAdmin);
        ordenCompraRepository.save(orden);
        FacturaCliente factura = ordenCompraService.buscarFacturaDePedido(idOrden).orElse(null);
        if (factura != null) {
            factura.setEstado(EstadoFactura.ANULADA);
            facturaClienteRepository.save(factura);
        }
    }
}
