package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CarritoService;
import com.zero.ecommerce.services.FormaDePagoService;
import com.zero.ecommerce.services.NotificacionCompraService;
import com.zero.ecommerce.services.OrdenCompraService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.StockService;
import com.zero.ecommerce.services.VentaService;

/**
 * Webhook de Mercado Pago (E4-10) con la base del seeder y la API de pagos mockeada. No es {@code @Transactional}
 * porque cada notificación corre en su propia transacción, igual que en producción.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "mercadopago.webhook-secret=" + PagoWebhookIntegrationTest.SECRETO })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PagoWebhookIntegrationTest {

    static final String SECRETO = "secreto-de-prueba";
    private static final String WEBHOOK = "/webhooks/mercadopago";
    private static final String REMERA = "REM-DRY-H-M";

    private final MockMvc mvc;
    private final VentaService ventaService;
    private final CarritoService carritoService;
    private final OrdenCompraService ordenCompraService;
    private final FormaDePagoService formaDePagoService;
    private final ProductoService productoService;
    private final StockService stockService;
    @MockitoBean
    private PaymentClient paymentClient;
    @MockitoBean
    private NotificacionCompraService notificacionCompraService;

    PagoWebhookIntegrationTest(@Autowired MockMvc mvc, @Autowired VentaService ventaService,
            @Autowired CarritoService carritoService, @Autowired OrdenCompraService ordenCompraService,
            @Autowired FormaDePagoService formaDePagoService, @Autowired ProductoService productoService,
            @Autowired StockService stockService) {
        this.mvc = mvc;
        this.ventaService = ventaService;
        this.carritoService = carritoService;
        this.ordenCompraService = ordenCompraService;
        this.formaDePagoService = formaDePagoService;
        this.productoService = productoService;
        this.stockService = stockService;
    }

    @Test
    void unPagoAprobadoNotificadoDosVecesDejaLaFacturaPagadaYDescuentaElStockUnaSolaVez() throws Exception {
        FacturaCliente factura = comprarDosRemerasConMercadoPago("ORD-DEMO0001");
        String idOrden = factura.getOrdenCompra().getId();
        int remerasAntes = stock(REMERA);
        pagoEnMercadoPago(111L, "approved", factura);

        mvc.perform(notificacion("111")).andExpect(status().isOk());
        mvc.perform(notificacion("111")).andExpect(status().isOk());

        assertThat(ordenCompraService.buscarPedido(idOrden).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        assertThat(ordenCompraService.buscarFacturaDePedido(idOrden).orElseThrow().getEstado())
                .isEqualTo(EstadoFactura.PAGADA);
        assertThat(stock(REMERA)).isEqualTo(remerasAntes - 2);
    }

    @Test
    void unPagoRechazadoDejaLaOrdenPendienteDePago() throws Exception {
        FacturaCliente factura = comprarDosRemerasConMercadoPago("ORD-DEMO0002");
        pagoEnMercadoPago(222L, "rejected", factura);

        mvc.perform(notificacion("222")).andExpect(status().isOk());

        assertThat(ordenCompraService.buscarPedido(factura.getOrdenCompra().getId()).getEstadoOrdenCompra())
                .isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
    }

    @Test
    void unaNotificacionConFirmaInvalidaSeRechazaSinConsultarElPago() throws Exception {
        mvc.perform(post(WEBHOOK).param("data.id", "333").param("type", "payment")
                        .header("x-request-id", "req-1")
                        .header("x-signature", "ts=" + System.currentTimeMillis() + ",v1=firma-falsa"))
                .andExpect(status().isUnauthorized());

        verify(paymentClient, never()).get(anyLong());
    }

    @Test
    void unaNotificacionQueNoEsDePagoSeIgnora() throws Exception {
        mvc.perform(post(WEBHOOK).param("topic", "merchant_order").param("id", "444"))
                .andExpect(status().isOk());

        verify(paymentClient, never()).get(anyLong());
    }

    private FacturaCliente comprarDosRemerasConMercadoPago(String pedidoDelCliente) throws ErrorServiceException {
        String idCliente = ordenCompraService.listarPedidoActivo().stream()
                .filter(o -> pedidoDelCliente.equals(o.getIdentificadorCompra()))
                .map(o -> o.getCliente().getId())
                .findFirst().orElseThrow();
        carritoService.agregarProducto(idCliente, productoService.buscarProductoPorCodigo(REMERA).getId(), 2);
        String idMercadoPago = formaDePagoService.listarFormaDePagoActivo().stream()
                .filter(f -> f.getTipoPago() == TipoPago.BILLETERA_VIRTUAL)
                .findFirst().orElseThrow().getId();
        return ventaService.confirmarCompra(idCliente, idMercadoPago);
    }

    private void pagoEnMercadoPago(long id, String estado, FacturaCliente factura) throws Exception {
        Payment pago = mock(Payment.class);
        when(pago.getId()).thenReturn(id);
        when(pago.getStatus()).thenReturn(estado);
        when(pago.getExternalReference()).thenReturn(factura.getOrdenCompra().getIdentificadorCompra());
        when(pago.getTransactionAmount()).thenReturn(BigDecimal.valueOf(factura.calcularTotal()));
        when(pago.getCurrencyId()).thenReturn("ARS");
        when(paymentClient.get(id)).thenReturn(pago);
    }

    /** Notificación con el formato de Webhooks y la firma x-signature que calcula Mercado Pago. */
    private MockHttpServletRequestBuilder notificacion(String idPago) throws Exception {
        String requestId = "req-" + idPago;
        long ts = System.currentTimeMillis();
        String manifest = "id:" + idPago + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String v1 = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        return post(WEBHOOK).param("data.id", idPago).param("type", "payment")
                .header("x-request-id", requestId)
                .header("x-signature", "ts=" + ts + ",v1=" + v1);
    }

    private int stock(String codigo) throws ErrorServiceException {
        return stockService.buscarStockActual(productoService.buscarProductoPorCodigo(codigo).getId());
    }
}
