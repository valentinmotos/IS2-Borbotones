package com.dulcemayor.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Validates Mercado Pago x-signature using the data.id, x-request-id and timestamp manifest. */
public final class MercadoPagoWebhookVerifier {
    private MercadoPagoWebhookVerifier() {}

    public static boolean esValida(String firma, String requestId, String dataId, String secreto) {
        if (vacio(firma) || vacio(requestId) || vacio(dataId) || vacio(secreto)) return false;
        String timestamp = null;
        String firmaRecibida = null;
        for (String parte : firma.split(",")) {
            String[] claveValor = parte.trim().split("=", 2);
            if (claveValor.length != 2) continue;
            if ("ts".equals(claveValor[0])) timestamp = claveValor[1];
            if ("v1".equals(claveValor[0])) firmaRecibida = claveValor[1];
        }
        if (vacio(timestamp) || vacio(firmaRecibida)) return false;
        String manifiesto = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperada = mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8));
            byte[] recibida = HexFormat.of().parseHex(firmaRecibida);
            return MessageDigest.isEqual(esperada, recibida);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}
