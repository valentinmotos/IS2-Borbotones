package com.zero.ecommerce.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Arma el CSV de los reportes (E5-02). Sale en UTF-8 con BOM, separado por punto y coma y con coma decimal, que es lo
 * que espera Excel con la configuración regional de Argentina: así muestra bien las tildes y cada dato en su columna.
 */
public final class ExportadorCsv {

    private static final byte[] BOM_UTF8 = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private static final String SEPARADOR = ";";
    private static final String FIN_DE_LINEA = "\r\n";
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExportadorCsv() {
    }

    /** Devuelve el CSV con una línea de encabezados y una línea por fila. */
    public static byte[] exportar(List<String> encabezados, List<List<String>> filas) {
        StringBuilder csv = new StringBuilder();
        agregarLinea(csv, encabezados);
        for (List<String> fila : filas) {
            agregarLinea(csv, fila);
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.writeBytes(BOM_UTF8);
        salida.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return salida.toByteArray();
    }

    /** Un monto con dos decimales y coma decimal, por ejemplo "1500,50". */
    public static String monto(double valor) {
        return String.format(ARGENTINA, "%.2f", valor);
    }

    /** Una fecha como dd/MM/yyyy, o vacío si no hay. */
    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    private static void agregarLinea(StringBuilder csv, List<String> campos) {
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) {
                csv.append(SEPARADOR);
            }
            csv.append(escapar(campos.get(i)));
        }
        csv.append(FIN_DE_LINEA);
    }

    /**
     * RFC 4180: si el campo tiene el separador, comillas o saltos de línea, va entre comillas dobles y las comillas
     * internas se duplican.
     */
    private static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        if (campo.contains(SEPARADOR) || campo.contains("\"") || campo.contains("\n") || campo.contains("\r")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }
}
