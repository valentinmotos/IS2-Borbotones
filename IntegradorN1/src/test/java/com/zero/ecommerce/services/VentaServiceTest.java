package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private FacturaClienteRepository facturaClienteRepository;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private CarritoService carritoService;
    @Mock
    private ClienteService clienteService;
    @Mock
    private FormaDePagoService formaDePagoService;
    @Mock
    private StockService stockService;
    @Mock
    private VigenciaPrecioService vigenciaPrecioService;
    @Mock
    private NotificacionCompraService notificacionCompraService;

    @InjectMocks
    private VentaService service;

    private OrdenCompra orden;
    private FacturaCliente factura;

    @BeforeEach
    void setUp() {
        orden = new OrdenCompra();
        orden.setId("o1");
        factura = new FacturaCliente();
        factura.setEstado(EstadoFactura.SIN_DEFINIR);
    }

    @Test
    void elClienteAnulaUnaCompraSinPagarYSeAnulaTambienLaFactura() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.anularVenta("o1", false);

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.ANULADA);
        verify(ordenCompraRepository).save(orden);
        verify(facturaClienteRepository).save(factura);
        // Todavía no se había pagado: el stock no se descontó y no hay nada que reingresar.
        verify(stockService, never()).revertirMovimiento(any());
    }

    @Test
    void registrarPagoPasaOrdenAPendienteEnvioYFacturaAPagada() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        Producto producto = producto("p1", "Remera");
        factura.agregarDetalle(producto, 2, 1000.0);

        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.registrarPago("o1");

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        verify(ordenCompraRepository).save(orden);
        verify(facturaClienteRepository).save(factura);
        verify(stockService).registrarMovimiento(any());
    }

    @Test
    void registrarPagoEsIdempotenteSiLaOrdenYaEstaEnPendienteEnvio() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENVIO);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);

        service.registrarPago("o1");

        verify(ordenCompraRepository, never()).save(any());
        verify(facturaClienteRepository, never()).save(any());
    }

    @Test
    void elClienteNoPuedeAnularUnaCompraPagada() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENVIO);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);

        assertThatThrownBy(() -> service.anularVenta("o1", false))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue pagada. Para anularla comunicate con la tienda.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        verify(ordenCompraRepository, never()).save(any());
    }

    // ----- registrarPago y anulación del admin (E4-03 / E4-07) -----

    @Test
    void elAdministradorAnulaUnaVentaPagadaYReponeElStock() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENVIO);
        factura.setEstado(EstadoFactura.PAGADA);
        factura.agregarDetalle(new Producto(), 2, 1500);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.anularVenta("o1", true);

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.ANULADA);
        verify(stockService).revertirMovimiento(factura.getDetalles().get(0));
        verify(notificacionCompraService).notificarCambioEstado(orden);
    }

    @Test
    void registrarPagoActualizaOrdenFacturaStockYNotifica() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        Producto producto = new Producto();
        factura.agregarDetalle(producto, 2, 1500);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.registrarPago("o1");

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        assertThat(factura.getTotalPagado()).isEqualTo(3000);
        verify(stockService).registrarMovimiento(factura.getDetalles().get(0));
        verify(notificacionCompraService).notificarCambioEstado(orden);
    }

    @Test
    void registrarDosVecesElMismoPagoNoDuplicaStockNiCorreo() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENVIO);
        factura.setEstado(EstadoFactura.PAGADA);
        Producto producto = new Producto();
        factura.agregarDetalle(producto, 2, 1500);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.registrarPago("o1");
        service.registrarPago("o1");

        verify(stockService, never()).registrarMovimiento(any());
        verify(notificacionCompraService, never()).notificarCambioEstado(any());
        verify(ordenCompraService, times(2)).buscarPedido("o1");
    }

    @Test
    void siUnProductoSeQuedoSinStockElPagoNoSeRegistraNiSeAvisa() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        DetalleFactura remera = factura.agregarDetalle(producto("p1", "Remera"), 5, 1000);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));
        when(stockService.registrarMovimiento(remera))
                .thenThrow(new ErrorServiceException("No hay stock suficiente de Remera (talle M): hay 2 y se necesitan 5."));

        assertThatThrownBy(() -> service.registrarPago("o1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No hay stock suficiente de Remera (talle M): hay 2 y se necesitan 5.");
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.SIN_DEFINIR);
        verify(facturaClienteRepository, never()).save(any());
        verify(notificacionCompraService, never()).notificarCambioEstado(any());
    }

    @Test
    void unPedidoSinFacturaNoSePuedePagar() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarPago("o1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El pedido no tiene una factura asociada.");
    }

    @Test
    void unaVentaAnuladaNoSePuedePagar() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.ANULADA);
        factura.setEstado(EstadoFactura.ANULADA);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        assertThatThrownBy(() -> service.registrarPago("o1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede registrar el pago de una factura anulada.");
        verify(stockService, never()).registrarMovimiento(any());
    }

    // ----- confirmarCompra (E4-02) -----

    @Test
    void confirmarLaCompraPasaLaOrdenAPendienteDePagoYCreaLaFacturaConLosPreciosVigentes() throws Exception {
        Cliente cliente = clienteConUsuario();
        FormaDePago transferencia = formaDePago(TipoPago.TRANSFERENCIA);
        Producto remera = producto("p1", "Remera");
        OrdenCompra carrito = carritoCon(remera, 2, 1000);
        when(clienteService.buscarCliente("c1")).thenReturn(cliente);
        when(clienteService.perfilCompleto("u1")).thenReturn(true);
        when(formaDePagoService.buscarFormaDePago("f1")).thenReturn(transferencia);
        when(carritoService.obtenerCarrito("c1")).thenReturn(carrito);
        when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1200.0);
        when(stockService.buscarStockActual("p1")).thenReturn(5);
        when(facturaClienteRepository.findFirstByOrderByNumeroFacturaDesc()).thenReturn(Optional.of(facturaNumero(8)));

        FacturaCliente venta = service.confirmarCompra("c1", "f1");

        assertThat(carrito.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
        assertThat(carrito.getTotal()).isEqualTo(2400);
        assertThat(venta.getNumeroFactura()).isEqualTo(9);
        assertThat(venta.getEstado()).isEqualTo(EstadoFactura.SIN_DEFINIR);
        assertThat(venta.getCliente()).isSameAs(cliente);
        assertThat(venta.getOrdenCompra()).isSameAs(carrito);
        assertThat(venta.getFormaDePago()).isSameAs(transferencia);
        assertThat(venta.getDetalles()).singleElement().satisfies(d -> {
            assertThat(d.getProducto()).isSameAs(remera);
            assertThat(d.getCantidad()).isEqualTo(2);
            assertThat(d.getPrecioUnitario()).isEqualTo(1200);
        });
        assertThat(venta.getTotalPagado()).isEqualTo(2400);
        verify(facturaClienteRepository).save(venta);
        verify(notificacionCompraService).enviarConfirmacion(carrito);
    }

    @Test
    void laPrimeraVentaEsLaNumeroUno() throws Exception {
        prepararCompraValida();
        when(facturaClienteRepository.findFirstByOrderByNumeroFacturaDesc()).thenReturn(Optional.empty());

        assertThat(service.confirmarCompra("c1", "f1").getNumeroFactura()).isEqualTo(1);
    }

    @Test
    void sinPerfilCompletoNoSePuedeConfirmar() throws Exception {
        when(clienteService.buscarCliente("c1")).thenReturn(clienteConUsuario());
        when(clienteService.perfilCompleto("u1")).thenReturn(false);

        assertThatThrownBy(() -> service.confirmarCompra("c1", "f1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage(VentaService.MENSAJE_PERFIL_INCOMPLETO);
        verify(facturaClienteRepository, never()).save(any());
    }

    @Test
    void sinFormaDePagoNoSePuedeConfirmar() throws Exception {
        when(clienteService.buscarCliente("c1")).thenReturn(clienteConUsuario());
        when(clienteService.perfilCompleto("u1")).thenReturn(true);

        assertThatThrownBy(() -> service.confirmarCompra("c1", " "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Elegí una forma de pago.");
    }

    @Test
    void siNoAlcanzaElStockNoSeConfirmaNiSeCreaLaFactura() throws Exception {
        OrdenCompra carrito = prepararCompraValida();
        when(stockService.buscarStockActual("p1")).thenReturn(1);

        assertThatThrownBy(() -> service.confirmarCompra("c1", "f1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No hay stock suficiente de \"Remera\"");
        assertThat(carrito.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        verify(facturaClienteRepository, never()).save(any());
        verify(notificacionCompraService, never()).enviarConfirmacion(any());
    }

    @Test
    void unCarritoVacioNoSePuedeConfirmar() throws Exception {
        prepararCompraValida().getDetalles().clear();

        assertThatThrownBy(() -> service.confirmarCompra("c1", "f1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede confirmar un carrito vacío.");
        verify(facturaClienteRepository, never()).save(any());
    }

    private OrdenCompra prepararCompraValida() throws Exception {
        OrdenCompra carrito = carritoCon(producto("p1", "Remera"), 2, 1000);
        when(clienteService.buscarCliente("c1")).thenReturn(clienteConUsuario());
        when(clienteService.perfilCompleto("u1")).thenReturn(true);
        when(formaDePagoService.buscarFormaDePago("f1")).thenReturn(formaDePago(TipoPago.EFECTIVO));
        when(carritoService.obtenerCarrito("c1")).thenReturn(carrito);
        lenient().when(vigenciaPrecioService.buscarPrecioVigente("p1")).thenReturn(1000.0);
        lenient().when(stockService.buscarStockActual("p1")).thenReturn(5);
        return carrito;
    }

    private Cliente clienteConUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setUsuario(usuario);
        return cliente;
    }

    private FormaDePago formaDePago(TipoPago tipo) {
        FormaDePago forma = new FormaDePago();
        forma.setId("f1");
        forma.setTipoPago(tipo);
        forma.setObservacion(tipo.getDescripcion());
        return forma;
    }

    private Producto producto(String id, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setTalle("M");
        return producto;
    }

    private OrdenCompra carritoCon(Producto producto, int cantidad, double precio) {
        OrdenCompra carrito = new OrdenCompra();
        carrito.setId("o2");
        carrito.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        carrito.crearDetalle(producto, cantidad, precio);
        return carrito;
    }

    private FacturaCliente facturaNumero(long numero) {
        FacturaCliente anterior = new FacturaCliente();
        anterior.setNumeroFactura(numero);
        return anterior;
    }
}
