package com.zero.ecommerce.utils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Funciones de texto reutilizables por los services. No tiene estado: solo métodos estáticos.
 */
public final class TextoUtils {

    private TextoUtils() {
    }

    /**
     * Compara nombres sin importar mayúsculas, tildes ni espacios en los extremos, así
     * "Guaymallén" y "GUAYMALLEN" cuentan como iguales. Se compara en Java porque
     * SQLite no normaliza Unicode (ver NacionalidadService).
     */
    public static boolean mismoNombre(String a, String b) {
        return a != null && b != null && normalizar(a).equals(normalizar(b));
    }

    /** Quita espacios en los extremos y tildes, y pasa a minúsculas: "  Perú " → "peru". */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
