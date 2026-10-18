package com.zero.ecommerce.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Funciones de texto reutilizables por los services. No tiene estado: solo métodos estáticos.
 */
public final class TextoUtils {

    // Formato básico usuario@dominio.ext: la verificación real es que el correo llegue.
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private TextoUtils() {
    }

    /** true si el texto tiene formato de correo electrónico (usuario@dominio.ext). */
    public static boolean esCorreoValido(String correo) {
        return correo != null && CORREO.matcher(correo.strip()).matches();
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
