package com.dulcemayor.dto;

/** E4-08: archivo generado en memoria (p. ej. una factura PDF) y el nombre con que se descarga. */
public record ArchivoDescargable(String nombreArchivo, byte[] contenido) {
}
