package com.dulcemayor.service.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dulcemayor.exception.ServicioNoDisponibleException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoOrdersRestClientTest {
    @Test
    void enviaCreateOrderAlEndpointOficialConAccessTokenEIdempotencyKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MercadoPagoOrdersRestClient client = new MercadoPagoOrdersRestClient(
                builder.baseUrl("https://api.mercadopago.com").build(), "TEST-ACCESS-TOKEN");
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-ACCESS-TOKEN"))
                .andExpect(header("X-Idempotency-Key", "attempt-123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "type":"online",
                          "processing_mode":"manual",
                          "capture_mode":"automatic_async",
                          "total_amount":"1500.00",
                          "external_reference":"ref-123",
                          "items":[{"title":"Factura 42","unit_price":"1500.00","quantity":1}]
                        }
                        """))
                .andRespond(withSuccess("""
                        {"id":"order-123","status":"created","checkout_url":"https://mp.test/checkout",
                         "external_reference":"ref-123","total_amount":"1500.00","type":"online"}
                        """, MediaType.APPLICATION_JSON));

        var response = client.crearOrden(new MercadoPagoOrdersClient.CreateOrderRequest("online", "manual",
                "automatic_async", "1500.00", "ref-123", "Factura 42", null,
                List.of(new MercadoPagoOrdersClient.Item("Factura 42", "1500.00", 1)), null), "attempt-123");

        assertThat(response.id()).isEqualTo("order-123");
        assertThat(response.checkoutUrl()).isEqualTo("https://mp.test/checkout");
        server.verify();
    }

    @Test
    void noIntentaLlamarAlProveedorSiElAccessTokenEstaVacio() {
        MercadoPagoOrdersRestClient client = new MercadoPagoOrdersRestClient(
                RestClient.builder(), "https://api.mercadopago.com", " ");

        assertThatThrownBy(() -> client.buscarOrden("order-123"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("MERCADOPAGO_ACCESS_TOKEN");
    }
}
