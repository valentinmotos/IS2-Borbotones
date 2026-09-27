package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.StockService;
import com.zero.ecommerce.services.VentaService;

/**
 * Registro del pago, descuento de stock y anulación de ventas (E4-03), con los clientes del seeder. No es
 * {@code @Transactional}: cada llamada al service corre en su propia transacción, así se ve el rollback real cuando
 * falta stock. Por eso el contexto se descarta al terminar y no ensucia la base de las otras clases de test.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VentaIntegrationTest {

    private static final String GORRA = "GOR-TRN-U";
    private static final String REMERA = "REM-DRY-H-M";
    private static final String RELOJ = "REL-SPT-U";

    private final VentaService ventaService;
    private final CarritoService carritoService;
    private final OrdenCompraService ordenCompraService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private final StockService stockService;

    VentaIntegrationTest(@Autowired VentaService ventaService, @Autowired CarritoService carritoService,
            @Autowired OrdenCompraService ordenCompraService, @Autowired FormaDePagoService formaDePagoService,
            @Autowired ProductoService productoService, @Autowired StockService stockService) {
        this.ventaService = ventaService;
        this.carritoService = carritoService;
        this.ordenCompraService = ordenCompraService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
        this.stockService = stockService;
    }

    @Test
    void compraPagoSegundoPagoSinEfectoYAnulacionDelAdminQueReingresaElStock() throws Exception {
        // Lucía tiene en el carrito del seeder 1 gorra; le suma 2 remeras.
        String lucia = idCliente("ORD-DEMO0001");
        carritoService.agregarProducto(lucia, idProducto(REMERA), 2);
        int gorrasAntes = stock(GORRA);
        int remerasAntes = stock(REMERA);

        String idOrden = ventaService.confirmarCompra(lucia, idFormaDePago(TipoPago.EFECTIVO)).getOrdenCompra().getId();
        // Confirmar no mueve el stock: baja recién con el pago (RF13).
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes);

        ventaService.registrarPago(idOrden);
        assertThat(estadoOrden(idOrden)).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(estadoFactura(idOrden)).isEqualTo(EstadoFactura.PAGADA);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes - 1);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes - 2);

        // Mercado Pago puede notificar dos veces el mismo pago: no se descuenta de nuevo.
        ventaService.registrarPago(idOrden);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes - 1);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes - 2);

        // El cliente ya no puede anular una compra pagada; el administrador sí, y el stock vuelve.
        assertThatThrownBy(() -> ventaService.anularVenta(idOrden, false)).isInstanceOf(ErrorServiceException.class);
        ventaService.anularVenta(idOrden, true);
        assertThat(estadoOrden(idOrden)).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(estadoFactura(idOrden)).isEqualTo(EstadoFactura.ANULADA);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes);
    }

    @Test
    void siUnProductoSeQuedoSinStockElPagoNoAplicaNada() throws Exception {
        // Martín confirma dos compras con relojes: la primera se lleva todo el stock y la segunda, uno. Las dos se
        // confirman porque el stock baja recién con el pago.
        String martin = idCliente("ORD-DEMO0002");
        int relojes = stock(RELOJ);
        carritoService.agregarProducto(martin, idProducto(GORRA), 1);
        carritoService.agregarProducto(martin, idProducto(RELOJ), relojes);
        String todoElStock = ventaService.confirmarCompra(martin, idFormaDePago(TipoPago.TRANSFERENCIA))
                .getOrdenCompra().getId();
        carritoService.agregarProducto(martin, idProducto(RELOJ), 1);
        String unReloj = ventaService.confirmarCompra(martin, idFormaDePago(TipoPago.TRANSFERENCIA))
                .getOrdenCompra().getId();

        ventaService.registrarPago(unReloj);
        int gorrasAntes = stock(GORRA);

        assertThatThrownBy(() -> ventaService.registrarPago(todoElStock))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("No hay stock suficiente de Reloj Zero Sport");
        // No se aplicó nada: ni el pago, ni el descuento de la gorra.
        assertThat(estadoOrden(todoElStock)).isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
        assertThat(estadoFactura(todoElStock)).isEqualTo(EstadoFactura.SIN_DEFINIR);
        assertThat(stock(GORRA)).isEqualTo(gorrasAntes);
        assertThat(stock(RELOJ)).isEqualTo(relojes - 1);
    }

    private int stock(String codigo) throws ErrorServiceException {
        return stockService.buscarStockActual(idProducto(codigo));
    }

    private String idProducto(String codigo) throws ErrorServiceException {
        return productoService.buscarProductoPorCodigo(codigo).getId();
    }

    private EstadoOrdenCompra estadoOrden(String idOrden) throws ErrorServiceException {
        return ordenCompraService.buscarPedido(idOrden).getEstadoOrdenCompra();
    }

    private EstadoFactura estadoFactura(String idOrden) {
        return ordenCompraService.buscarFacturaDePedido(idOrden).orElseThrow().getEstado();
    }

    private String idCliente(String identificadorPedido) {
        return ordenCompraService.listarPedidoActivo().stream()
                .filter(o -> identificadorPedido.equals(o.getIdentificadorCompra()))
                .map(o -> o.getCliente().getId())
                .findFirst().orElseThrow();
    }

    private String idFormaDePago(TipoPago tipo) {
        return formaDePagoService.listarFormaDePagoActivo().stream()
                .filter(f -> f.getTipoPago() == tipo)
                .findFirst().orElseThrow().getId();
    }
}
