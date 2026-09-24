package com.zero.ecommerce.exception;

/**
 * Excepción de negocio que lanzan los services (por ejemplo, desde validar(...)).
 * El mensaje tiene que ser legible en español, porque se muestra tal cual al usuario.
 */
public class ErrorServiceException extends Exception {

    public ErrorServiceException(String mensaje) {
        super(mensaje);
    }
}
