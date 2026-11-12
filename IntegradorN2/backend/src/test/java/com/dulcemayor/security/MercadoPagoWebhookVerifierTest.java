package com.dulcemayor.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MercadoPagoWebhookVerifierTest {
    @Test
    void aceptaFirmaHmacValidaYRechazaCambiosEnLosDatosFirmados() throws Exception {
        String dataId = "ORDEN-ABC";
        String requestId = "solicitud-123";
        String timestamp = "1742505638683";
        String secret = "clave-prueba";
        String manifest = "id:orden-abc;request-id:" + requestId + ";ts:" + timestamp + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "ts=" + timestamp + ",v1="
                + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        assertThat(MercadoPagoWebhookVerifier.esValida(signature, requestId, dataId, secret)).isTrue();
        assertThat(MercadoPagoWebhookVerifier.esValida(signature, requestId, "otra-orden", secret)).isFalse();
        assertThat(MercadoPagoWebhookVerifier.esValida(signature, requestId, dataId, "otra-clave")).isFalse();
    }
}
