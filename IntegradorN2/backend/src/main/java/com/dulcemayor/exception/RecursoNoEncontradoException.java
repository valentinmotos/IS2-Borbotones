package com.dulcemayor.exception;

/**
 * El registro pedido no existe o está dado de baja. Se responde con 404 Not Found.
 * Ejemplo: throw new RecursoNoEncontradoException("No se encontró la categoría.");
 */
public class RecursoNoEncontradoException extends ErrorServiceException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String mensaje, String campo) {
        super(mensaje, campo);
    }
}
