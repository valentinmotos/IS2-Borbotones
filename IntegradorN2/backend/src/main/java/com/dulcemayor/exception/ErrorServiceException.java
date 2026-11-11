package com.dulcemayor.exception;

/**
 * Excepción de negocio que lanzan los services (por ejemplo, desde validar(...)).
 * El mensaje tiene que ser legible en español, porque se muestra tal cual al usuario.
 * El campo (opcional) indica qué dato del formulario causó el error, para marcarlo en la pantalla.
 *
 * Es no verificada (a diferencia de IntegradorN1) para que @Transactional deshaga los cambios
 * sin configuración extra. El código copiado de N1 que declara "throws ErrorServiceException"
 * sigue compilando igual.
 *
 * GlobalExceptionHandler la responde con 400. Subclases: DuplicadoException (409) y
 * RecursoNoEncontradoException (404) y ServicioNoDisponibleException (503).
 */
public class ErrorServiceException extends RuntimeException {

    private final String campo;

    public ErrorServiceException(String mensaje) {
        this(mensaje, null);
    }

    public ErrorServiceException(String mensaje, String campo) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
