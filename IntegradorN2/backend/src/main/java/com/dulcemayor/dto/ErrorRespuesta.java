package com.dulcemayor.dto;

/**
 * Cuerpo JSON uniforme de todas las respuestas de error de la API (contrato "Errores" del kickoff).
 * Ejemplo: {"mensaje": "Ya existe una categoría con ese nombre.", "campo": "nombre"}
 *
 * @param mensaje texto en español para mostrar al usuario
 * @param campo   dato del formulario que causó el error, o null si no corresponde a uno
 */
public record ErrorRespuesta(String mensaje, String campo) {

    public static ErrorRespuesta de(String mensaje) {
        return new ErrorRespuesta(mensaje, null);
    }
}
