package com.zero.ecommerce.utils;

/**
 * Validación del CUIT/CUIL argentino: 11 dígitos con dígito verificador (módulo 11).
 * Acepta el número con o sin guiones y lo devuelve normalizado como XX-XXXXXXXX-X.
 */
public final class CuitUtils {

    private static final int[] PESOS = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

    private CuitUtils() {
    }

    public static boolean esValido(String cuit) {
        if (cuit == null) {
            return false;
        }
        String digitos = cuit.strip();
        if (!digitos.matches("\\d{11}|\\d{2}-\\d{8}-\\d")) {
            return false;
        }
        digitos = digitos.replace("-", "");
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += Character.getNumericValue(digitos.charAt(i)) * PESOS[i];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            verificador = 0;
        }
        // Un resultado de 10 no tiene dígito verificador posible: el número no es válido.
        return verificador != 10 && verificador == Character.getNumericValue(digitos.charAt(10));
    }

    /** Devuelve el CUIT con el formato XX-XXXXXXXX-X. Llamar solo con un CUIT válido. */
    public static String normalizar(String cuit) {
        String digitos = cuit.strip().replace("-", "");
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }
}
