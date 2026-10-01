package com.zero.ecommerce.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Empleado;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;

/** Orquesta las acciones manuales de un empleado sobre un pedido desde el panel (E4-07). */
@Service
@Transactional(readOnly = true)
public class PedidoAccionService {

    private static final int LARGO_MAXIMO_MOTIVO = 500;

    private final OrdenCompraService ordenCompraService;
    private final VentaService ventaService;
    private final EmpleadoService empleadoService;
    private final OrdenCompraRepository ordenCompraRepository;
    private final FacturaClienteRepository facturaClienteRepository;
    private final NotificacionCompraService notificacionCompraService;

    public PedidoAccionService(OrdenCompraService ordenCompraService, VentaService ventaService,
            EmpleadoService empleadoService, OrdenCompraRepository ordenCompraRepository,
            FacturaClienteRepository facturaClienteRepository, NotificacionCompraService notificacionCompraService) {
        this.ordenCompraService = ordenCompraService;
        this.ventaService = ventaService;
        this.empleadoService = empleadoService;
        this.ordenCompraRepository = ordenCompraRepository;
        this.facturaClienteRepository = facturaClienteRepository;
        this.notificacionCompraService = notificacionCompraService;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public OrdenCompra confirmarPago(String idOrden, String correoEmpleado) throws ErrorServiceException {
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        FacturaCliente factura = buscarFactura(idOrden);
        TipoPago tipoPago = factura.getFormaDePago() == null ? null : factura.getFormaDePago().getTipoPago();
        if (tipoPago != TipoPago.EFECTIVO && tipoPago != TipoPago.TRANSFERENCIA
                && tipoPago != TipoPago.BILLETERA_VIRTUAL) {
            throw new ErrorServiceException(
                    "La forma de pago del pedido no admite confirmación manual.");
        }
        if (orden.getEstadoOrdenCompra() != com.zero.ecommerce.entities.enums.EstadoOrdenCompra.PENDIENTE_PAGO) {
            throw new ErrorServiceException("Solo se puede confirmar un pedido pendiente de pago.");
        }
        asociarEmpleado(factura, correoEmpleado);
        ventaService.registrarPago(idOrden);
        return orden;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public OrdenCompra marcarEnviado(String idOrden, String correoEmpleado) throws ErrorServiceException {
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        FacturaCliente factura = buscarFactura(idOrden);
        orden.marcarEnviado();
        asociarEmpleado(factura, correoEmpleado);
        ordenCompraRepository.save(orden);
        notificacionCompraService.notificarCambioEstado(orden);
        return orden;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public OrdenCompra marcarEntregado(String idOrden, String correoEmpleado) throws ErrorServiceException {
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        FacturaCliente factura = buscarFactura(idOrden);
        orden.marcarEntregado();
        asociarEmpleado(factura, correoEmpleado);
        ordenCompraRepository.save(orden);
        notificacionCompraService.notificarCambioEstado(orden);
        return orden;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public OrdenCompra anular(String idOrden, String correoEmpleado, String motivo) throws ErrorServiceException {
        validarMotivo(motivo);
        OrdenCompra orden = ordenCompraService.buscarPedido(idOrden);
        FacturaCliente factura = buscarFactura(idOrden);
        asociarEmpleado(factura, correoEmpleado);
        ventaService.anularVenta(idOrden, true);
        return orden;
    }

    private void asociarEmpleado(FacturaCliente factura, String correoEmpleado) throws ErrorServiceException {
        Empleado empleado = empleadoService.buscarEmpleadoPorCorreo(correoEmpleado);
        factura.setEmpleado(empleado);
        facturaClienteRepository.save(factura);
    }

    private FacturaCliente buscarFactura(String idOrden) throws ErrorServiceException {
        return ordenCompraService.buscarFacturaDePedido(idOrden)
                .orElseThrow(() -> new ErrorServiceException("El pedido no tiene una factura asociada."));
    }

    private void validarMotivo(String motivo) throws ErrorServiceException {
        if (motivo == null || motivo.isBlank()) {
            throw new ErrorServiceException("Ingresá el motivo de la anulación.");
        }
        String valor = motivo.strip();
        if (valor.length() > LARGO_MAXIMO_MOTIVO) {
            throw new ErrorServiceException("El motivo no puede superar los " + LARGO_MAXIMO_MOTIVO + " caracteres.");
        }
    }
}
