package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.OrdenPagoMercadoPago;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.FormaDePagoRepository;
import com.dulcemayor.repository.OrdenPagoMercadoPagoRepository;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.OrderResponse;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.PaymentTransaction;
import com.dulcemayor.service.adapter.MercadoPagoOrdersClient.Transactions;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckoutMercadoPagoServiceTest {
    private static final String SECRET = "webhook-secret-test";
    private FacturaRepository facturas;
    private OrdenPagoMercadoPagoRepository ordenes;
    private FormaDePagoRepository formas;
    private PagoService pagos;
    private MercadoPagoOrdersClient mercadoPago;
    private CheckoutMercadoPagoService service;
    private Factura factura;

    @BeforeEach
    void preparar() {
        facturas = mock(FacturaRepository.class);
        ordenes = mock(OrdenPagoMercadoPagoRepository.class);
        formas = mock(FormaDePagoRepository.class);
        pagos = mock(PagoService.class);
        mercadoPago = mock(MercadoPagoOrdersClient.class);
        service = new CheckoutMercadoPagoService(facturas, ordenes, formas, pagos, mercadoPago,
                "", "", "", SECRET);
        factura = facturaPropia();
        when(facturas.buscarActivaParaActualizar("fac-1")).thenReturn(Optional.of(factura));
        when(ordenes.findFirstByFactura_IdAndEstadoInAndEliminadoFalseOrderByFechaCreacionDesc(eq("fac-1"), any()))
                .thenReturn(Optional.empty());
        FormaDePago wallet = new FormaDePago();
        wallet.setTipoPago(TipoPago.BILLETERA_VIRTUAL);
        when(formas.findByEliminadoFalse()).thenReturn(List.of(wallet));
        when(pagos.saldoPendienteEnPesos("fac-1")).thenReturn(new BigDecimal("1200.00"));
        when(ordenes.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void creaOrderOnlineEnPesosConIdempotenciaYDevuelveLaCheckoutUrl() {
        when(mercadoPago.crearOrden(any(), any())).thenAnswer(call -> {
            MercadoPagoOrdersClient.CreateOrderRequest request = call.getArgument(0);
            assertThat(request.type()).isEqualTo("online");
            assertThat(request.processingMode()).isEqualTo("manual");
            assertThat(request.totalAmount()).isEqualTo("1200.00");
            assertThat(request.payer().email()).isEqualTo("minorista@dulcemayor.com.ar");
            assertThat(request.items()).singleElement().satisfies(item -> {
                assertThat(item.quantity()).isEqualTo(1);
                assertThat(item.unitPrice()).isEqualTo("1200.00");
            });
            return new OrderResponse("order-123", "created", "created", "https://mp.test/checkout",
                    request.externalReference(), new BigDecimal("1200.00"), BigDecimal.ZERO, "online", null);
        });

        var respuesta = service.crearCheckout("fac-1", "user-1", "minorista@dulcemayor.com.ar");

        assertThat(respuesta.ordenMercadoPagoId()).isEqualTo("order-123");
        assertThat(respuesta.checkoutUrl()).isEqualTo("https://mp.test/checkout");
        assertThat(respuesta.moneda()).isEqualTo("ARS");
        verify(mercadoPago).crearOrden(any(), any());
    }

    @Test
    void noPermiteCrearCheckoutParaLaFacturaDeOtroMinorista() {
        assertThatThrownBy(() -> service.crearCheckout("fac-1", "otro-usuario", "otro@correo.com"))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("No se encontró la factura");
        verify(mercadoPago, never()).crearOrden(any(), any());
    }

    @Test
    void webhookConsultaLaOrderEnMercadoPagoYRegistraUnaSolaVezElPagoAcreditado() throws Exception {
        OrdenPagoMercadoPago local = new OrdenPagoMercadoPago();
        local.setFactura(factura);
        local.setOrdenExternaId("order-123");
        local.setReferenciaExterna("external-123");
        local.setEstado("created");
        local.setMonto(new BigDecimal("1200.00"));
        when(ordenes.buscarPorOrdenExternaParaActualizar("order-123")).thenReturn(Optional.of(local));
        when(mercadoPago.buscarOrden("order-123")).thenReturn(new OrderResponse("order-123", "processed",
                "accredited", null, "external-123", new BigDecimal("1200.00"), new BigDecimal("1200.00"),
                "online", new Transactions(List.of(new PaymentTransaction("payment-1", "processed",
                        "accredited", new BigDecimal("1200.00"))))));
        String requestId = "req-1";
        String signature = signature("order-123", requestId, SECRET);

        service.procesarWebhook("order-123", signature, requestId, "order-123");
        service.procesarWebhook("order-123", signature, requestId, "order-123");

        assertThat(local.getEstado()).isEqualTo("processed");
        assertThat(local.getPagoExternoId()).isEqualTo("payment-1");
        verify(pagos).registrarMercadoPago("fac-1", new BigDecimal("1200.00"), "payment-1");
        verify(mercadoPago).buscarOrden("order-123");
    }

    @Test
    void rechazaWebhookConFirmaIncorrectaSinConsultarLaApi() {
        assertThatThrownBy(() -> service.procesarWebhook("order-123", "ts=1,v1=00", "req-1", "order-123"))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("firma");
        verify(mercadoPago, never()).buscarOrden(any());
    }

    private static Factura facturaPropia() {
        Usuario usuario = new Usuario();
        usuario.setId("user-1");
        usuario.setNombreUsuario("minorista@dulcemayor.com.ar");
        Minorista minorista = new Minorista();
        minorista.setUsuario(usuario);
        PuntoDeVenta punto = new PuntoDeVenta();
        punto.setMinorista(minorista);
        Pedido pedido = new Pedido();
        pedido.setPuntoDeVenta(punto);
        Factura factura = new Factura();
        factura.setId("fac-1");
        factura.setPedido(pedido);
        factura.setNumeroFactura(123);
        factura.setTotal(1200);
        factura.setEstado(EstadoFactura.PENDIENTE_PAGO);
        return factura;
    }

    private static String signature(String dataId, String requestId, String secret) throws Exception {
        String timestamp = "1742505638683";
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "ts=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }
}
