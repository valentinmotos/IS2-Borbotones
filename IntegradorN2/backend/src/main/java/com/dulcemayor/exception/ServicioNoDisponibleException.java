package com.dulcemayor.exception;

/**
 * Un servicio externo del que depende la operación no respondió o respondió algo inutilizable
 * (por ejemplo, Ollama para el chatbot de E3-04). Se responde con 503 Service Unavailable, para que
 * el cliente muestre una alternativa (el formulario de presupuesto de E3-05).
 */
public class ServicioNoDisponibleException extends ErrorServiceException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
