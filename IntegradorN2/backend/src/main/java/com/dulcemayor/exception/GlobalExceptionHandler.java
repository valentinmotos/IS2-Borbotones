package com.dulcemayor.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.dulcemayor.dto.ErrorRespuesta;

/**
 * Traduce las excepciones de toda la API a la respuesta JSON uniforme {mensaje, campo}.
 * Los controllers no capturan excepciones: las dejan subir hasta acá.
 * Nunca se devuelven stack traces ni detalles internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- Errores de negocio (services) ----

    @ExceptionHandler(DuplicadoException.class)
    public ResponseEntity<ErrorRespuesta> duplicado(DuplicadoException ex) {
        return responder(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<ErrorRespuesta> noAutenticado(NoAutenticadoException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> servicioNoDisponible(ServicioNoDisponibleException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    @ExceptionHandler(ErrorServiceException.class)
    public ResponseEntity<ErrorRespuesta> negocio(ErrorServiceException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex);
    }

    // ---- Errores de la solicitud ----

    /** @Valid en un DTO de request: se informa el primer campo inválido con su mensaje en español. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        if (error == null) {
            return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.", null);
        }
        return responder(HttpStatus.BAD_REQUEST, error.getDefaultMessage(), error.getField());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato válido.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El valor de '" + ex.getName() + "' no es válido.", ex.getName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "Falta el parámetro '" + ex.getParameterName() + "'.", ex.getParameterName());
    }

    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    public ResponseEntity<ErrorRespuesta> rutaInexistente(Exception ex) {
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "La operación no está permitida en este recurso.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorRespuesta> archivoGrande(MaxUploadSizeExceededException ex) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (2 MB).", null);
    }

    // ---- Seguridad ----

    /**
     * Los errores de seguridad lanzados por @PreAuthorize se vuelven a lanzar para que los maneje
     * Spring Security: así un usuario sin sesión recibe 401 y uno sin permiso 403
     * (ver ErroresSeguridadHandler). Si se respondieran acá, todo sería 403.
     */
    @ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
    public void seguridad(RuntimeException ex) {
        throw ex;
    }

    // ---- Cualquier otro error ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex) {
        log.error("Error inesperado al atender la solicitud", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intentá de nuevo más tarde.", null);
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, ErrorServiceException ex) {
        return responder(estado, ex.getMessage(), ex.getCampo());
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje, String campo) {
        return ResponseEntity.status(estado).body(new ErrorRespuesta(mensaje, campo));
    }
}
