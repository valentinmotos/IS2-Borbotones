package com.zero.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;

/**
 * Clientes del SDK de Mercado Pago (E4-10). El access token es global en el SDK, así que se configura una sola vez al
 * arrancar. Los clientes son beans para poder reemplazarlos por mocks en los tests.
 */
@Configuration
public class MercadoPagoClientesConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoClientesConfig.class);

    public MercadoPagoClientesConfig(@Value("${mercadopago.access-token:}") String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            LOGGER.warn("Falta el access token de Mercado Pago (MP_ACCESS_TOKEN): no se van a poder crear pagos.");
        } else {
            MercadoPagoConfig.setAccessToken(accessToken);
        }
    }

    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }

    @Bean
    public PaymentRefundClient paymentRefundClient() {
        return new PaymentRefundClient();
    }
}
