package com.zero.ecommerce.utils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exportador CSV genérico para reportes (E5-02).
 * Genera un archivo CSV en UTF-8 con BOM (Byte Order Mark) para que Microsoft Excel
 * reconozca y muestre correctamente caracteres con tildes y símbolos especiales en español.
 */
public final class ExportadorCsv {

    private static final byte[] BOM_UTF8 = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    private ExportadorCsv() {
        // Clase de utilidad no instanciable
    }

    /**
     * Genera un arreglo de bytes con el contenido CSV en UTF-8 con BOM.
     *
     * @param encabezados Lista de nombres de columnas.
     * @param filas       Lista de filas, donde cada fila es una lista de valores de celdas.
     * @return Arreglo de bytes del CSV con BOM.
     */
    public static byte[] exportar(List<String> encabezados, List<List<String>> filas) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // Escribir el BOM UTF-8 (EF BB BF)
            baos.write(BOM_UTF8);

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
                if (encabezados != null && !encabezados.isEmpty()) {
                    writer.println(convertirFila(encabezados));
                }
                if (filas != null) {
                    for (List<String> fila : filas) {
                        writer.println(convertirFila(fila));
                    }
                }
                writer.flush();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar la exportación CSV: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    /**
     * Convierte una lista de celdas a una línea formateada en CSV, escapando comillas y caracteres especiales.
     */
    private static String convertirFila(List<String> campos) {
        if (campos == null || campos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escaparCampo(campos.get(i)));
        }
        return sb.toString();
    }

    /**
     * Escapa un campo CSV según la especificación RFC 4180:
     * Si contiene comas, comillas o saltos de línea, lo encierra entre comillas dobles
     * y duplica las comillas internas.
     */
    public static String escaparCampo(String campo) {
        if (campo == null) {
            return "\"\"";
        }
        String texto = campo;
        boolean requiereComillas = texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r");
        if (texto.contains("\"")) {
            texto = texto.replace("\"", "\"\"");
        }
        if (requiereComillas) {
            return "\"" + texto + "\"";
        }
        return texto;
    }
}
