package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;

@ExtendWith(MockitoExtension.class)
class MercadoPagoServiceTest {

    private static final long ID_PAGO = 123456789L;

    @Mock
    private PreferenceClient preferenceClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private PaymentRefundClient paymentRefundClient;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private VentaService ventaService;

    private MercadoPagoService service;
    private OrdenCompra orden;
    private FacturaCliente factura;

    @BeforeEach
    void setUp() {
        service = servicioCon("https://zero.example");
        orden = new OrdenCompra();
        orden.setId("o1");
        orden.setIdentificadorCompra("ORD-0001");
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        factura = new FacturaCliente();
        Producto remera = new Producto();
        remera.setId("p1");
        remera.setNombre("Remera");
        factura.agregarDetalle(remera, 2, 1500.0);
    }

    private MercadoPagoService servicioCon(String urlBase) {
        return new MercadoPagoService("APP_USR-TEST", urlBase, preferenceClient, paymentClient, paymentRefundClient,
                ordenCompraService, ventaService);
    }

    // ----- crearPreferencia -----

    @Test
    void crearPreferenciaOrdenNulaLanzaExcepcion() {
        assertThatThrownBy(() -> service.crearPreferencia(null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("nula");
    }

    @Test
    void crearPreferenciaSinAccessTokenLanzaExcepcion() {
        MercadoPagoService sinToken = new MercadoPagoService("", "https://zero.example", preferenceClient,
                paymentClient, paymentRefundClient, ordenCompraService, ventaService);

        assertThatThrownBy(() -> sinToken.crearPreferencia(orden))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void crearPreferenciaSinFacturaLanzaExcepcion() {
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearPreferencia(orden))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El pedido no tiene una factura asociada.");
    }

    @Test
    void crearPreferenciaArmaLosItemsYDevuelveElInitPoint() throws Exception {
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));
        Preference preference = mock(Preference.class);
        when(preference.getInitPoint()).thenReturn("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=1");
        when(preferenceClient.create(any(PreferenceRequest.class))).thenReturn(preference);

        String url = service.crearPreferencia(orden);

        ArgumentCaptor<PreferenceRequest> captor = ArgumentCaptor.forClass(PreferenceRequest.class);
        verify(preferenceClient).create(captor.capture());
        PreferenceRequest request = captor.getValue();
        assertThat(url).isEqualTo("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=1");
        assertThat(request.getExternalReference()).isEqualTo("ORD-0001");
        assertThat(request.getItems()).hasSize(1);
        assertThat(request.getItems().get(0).getId()).isEqualTo("p1");
        assertThat(request.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(request.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1500");
        assertThat(request.getItems().get(0).getCurrencyId()).isEqualTo("ARS");
        assertThat(request.getNotificationUrl())
                .isEqualTo("https://zero.example/webhooks/mercadopago?source_news=webhooks");
        assertThat(request.getBackUrls().getSuccess()).isEqualTo("https://zero.example/cliente/pago/resultado");
        assertThat(request.getAutoReturn()).isEqualTo("approved");
    }

    @Test
    void conUrlLocalNoMandaBackUrlsNiNotificationUrl() throws Exception {
        MercadoPagoService local = servicioCon("http://localhost:8080");
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));
        Preference preference = mock(Preference.class);
        when(preference.getInitPoint()).thenReturn("https://mp/checkout");
        when(preferenceClient.create(any(PreferenceRequest.class))).thenReturn(preference);

        local.crearPreferencia(orden);

        ArgumentCaptor<PreferenceRequest> captor = ArgumentCaptor.forClass(PreferenceRequest.class);
        verify(preferenceClient).create(captor.capture());
        assertThat(captor.getValue().getNotificationUrl()).isNull();
        assertThat(captor.getValue().getBackUrls()).isNull();
        assertThat(captor.getValue().getAutoReturn()).isNull();
    }

    // ----- procesarPago -----

    private Payment pago(String estado, String monto) {
        // Lenient: no todos los casos leen todos los datos del pago.
        Payment pago = mock(Payment.class, withSettings().strictness(Strictness.LENIENT));
        when(pago.getId()).thenReturn(ID_PAGO);
        when(pago.getStatus()).thenReturn(estado);
        when(pago.getExternalReference()).thenReturn("ORD-0001");
        when(pago.getTransactionAmount()).thenReturn(new BigDecimal(monto));
        when(pago.getCurrencyId()).thenReturn("ARS");
        return pago;
    }

    private void conPago(Payment pago) throws Exception {
        when(paymentClient.get(ID_PAGO)).thenReturn(pago);
        when(ordenCompraService.buscarPorIdentificadorCompra("ORD-0001")).thenReturn(Optional.of(orden));
    }

    @Test
    void unPagoAprobadoConElMontoCorrectoRegistraElPago() throws Exception {
        conPago(pago("approved", "3000.00"));
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(ventaService).registrarPago("o1");
        verify(paymentRefundClient, never()).refund(anyLong());
    }

    @Test
    void unPagoAprobadoConOtroMontoNoRegistraElPago() throws Exception {
        conPago(pago("approved", "1.00"));
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(ventaService, never()).registrarPago(anyString());
    }

    @Test
    void siLaOrdenNoAdmiteElPagoSeDevuelveElDineroYSeAnula() throws Exception {
        conPago(pago("approved", "3000"));
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));
        doThrow(new ErrorServiceException("No hay stock suficiente de Remera."))
                .when(ventaService).registrarPago("o1");

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(paymentRefundClient).refund(ID_PAGO);
        verify(ventaService).anularVenta("o1", true);
    }

    @Test
    void unPagoRechazadoNoCambiaLaOrden() throws Exception {
        conPago(pago("rejected", "3000"));

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(ventaService, never()).registrarPago(anyString());
        verify(ventaService, never()).anularVenta(anyString(), anyBoolean());
    }

    @Test
    void unPagoDevueltoAnulaLaOrdenPagada() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENVIO);
        conPago(pago("refunded", "3000"));

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(ventaService).anularVenta("o1", true);
    }

    @Test
    void unPagoDevueltoDeUnaOrdenEnviadaNoLaAnula() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENTREGA);
        conPago(pago("charged_back", "3000"));

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(ventaService, never()).anularVenta(anyString(), anyBoolean());
    }

    @Test
    void unIdNoNumericoNoConsultaLaApi() throws Exception {
        service.procesarPago("ORD-0001");

        verify(paymentClient, never()).get(anyLong());
        verify(ventaService, never()).registrarPago(anyString());
    }

    @Test
    void unPagoInexistenteSeIgnora() throws Exception {
        MPApiException noEncontrado = mock(MPApiException.class);
        when(noEncontrado.getStatusCode()).thenReturn(404);
        when(paymentClient.get(ID_PAGO)).thenThrow(noEncontrado);

        service.procesarPago(String.valueOf(ID_PAGO));

        verify(ventaService, never()).registrarPago(anyString());
    }

    @Test
    void siMercadoPagoFallaLanzaExcepcionParaQueReintente() throws Exception {
        MPApiException error = mock(MPApiException.class);
        when(error.getStatusCode()).thenReturn(500);
        when(paymentClient.get(ID_PAGO)).thenThrow(error);

        assertThatThrownBy(() -> service.procesarPago(String.valueOf(ID_PAGO)))
                .isInstanceOf(MPException.class);
    }
}
