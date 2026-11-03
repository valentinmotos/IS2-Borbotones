package com.dulcemayor.exception;

/**
 * Ya existe otro registro activo con el mismo dato único (nombre, código, CUIT, correo...).
 * Se responde con 409 Conflict.
 * Ejemplo: throw new DuplicadoException("Ya existe una categoría con ese nombre.", "nombre");
 */
public class DuplicadoException extends ErrorServiceException {

    public DuplicadoException(String mensaje) {
        super(mensaje);
    }

    public DuplicadoException(String mensaje, String campo) {
        super(mensaje, campo);
    }
}
