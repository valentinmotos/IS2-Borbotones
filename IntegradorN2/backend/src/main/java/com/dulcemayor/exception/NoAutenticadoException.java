package com.dulcemayor.exception;

/**
 * Credenciales incorrectas o cuenta que no puede ingresar (E1-01). Se responde con 401 Unauthorized.
 * Ejemplo: throw new NoAutenticadoException("Correo o clave incorrectos.");
 */
public class NoAutenticadoException extends ErrorServiceException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }

    public NoAutenticadoException(String mensaje, String campo) {
        super(mensaje, campo);
    }
}
