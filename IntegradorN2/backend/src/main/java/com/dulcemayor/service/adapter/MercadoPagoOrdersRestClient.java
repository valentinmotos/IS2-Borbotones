package com.dulcemayor.service.adapter;

import com.dulcemayor.exception.ServicioNoDisponibleException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** REST adapter for Checkout Pro through Mercado Pago's Orders API. */
@Component
public class MercadoPagoOrdersRestClient implements MercadoPagoOrdersClient {
    private final RestClient restClient;
    private final String accessToken;

    @Autowired
    public MercadoPagoOrdersRestClient(RestClient.Builder builder,
            @Value("${app.mercadopago.api-url:https://api.mercadopago.com}") String apiUrl,
            @Value("${app.mercadopago.access-token:}") String accessToken) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = builder.baseUrl(apiUrl).requestFactory(requestFactory).build();
        this.accessToken = limpiar(accessToken);
    }

    /** For tests: receive a RestClient already configured with a mock HTTP server. */
    MercadoPagoOrdersRestClient(RestClient restClient, String accessToken) {
        this.restClient = restClient;
        this.accessToken = limpiar(accessToken);
    }

    private static String limpiar(String accessToken) {
        return accessToken == null ? "" : accessToken.trim();
    }

    @Override
    public OrderResponse crearOrden(CreateOrderRequest request, String idempotencyKey) {
        verificarCredenciales();
        try {
            return restClient.post().uri("/v1/orders")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(OrderResponse.class);
        } catch (Exception ex) {
            throw new ServicioNoDisponibleException("Mercado Pago no pudo crear la orden de pago.");
        }
    }

    @Override
    public OrderResponse buscarOrden(String orderId) {
        verificarCredenciales();
        try {
            return restClient.get().uri("/v1/orders/{id}", orderId)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(OrderResponse.class);
        } catch (Exception ex) {
            throw new ServicioNoDisponibleException("No se pudo verificar la orden de pago en Mercado Pago.");
        }
    }

    private void verificarCredenciales() {
        if (accessToken.isBlank()) {
            throw new ServicioNoDisponibleException(
                    "Checkout de Mercado Pago no configurado: falta MERCADOPAGO_ACCESS_TOKEN.");
        }
    }
}
