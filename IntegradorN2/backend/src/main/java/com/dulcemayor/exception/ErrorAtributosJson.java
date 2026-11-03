package com.dulcemayor.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Errores que no pasan por los controllers (por ejemplo, una excepción en un filtro) terminan en
 * el /error de Spring Boot. Esta clase hace que también respondan {mensaje, campo} en lugar del
 * JSON por defecto (timestamp, status, error, path).
 */
@Component
public class ErrorAtributosJson extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
        Object estado = super.getErrorAttributes(request, ErrorAttributeOptions.defaults()).get("status");
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensajePara(estado instanceof Integer codigo ? codigo : 500));
        cuerpo.put("campo", null);
        return cuerpo;
    }

    private String mensajePara(int codigo) {
        HttpStatus estado = HttpStatus.resolve(codigo);
        if (estado == null || estado.is5xxServerError()) {
            return "Ocurrió un error inesperado. Intentá de nuevo más tarde.";
        }
        return switch (estado) {
            case NOT_FOUND -> "El recurso solicitado no existe.";
            case UNAUTHORIZED -> "Tenés que iniciar sesión para acceder a este recurso.";
            case FORBIDDEN -> "No tenés permisos para realizar esta acción.";
            case PAYLOAD_TOO_LARGE -> "El archivo supera el tamaño máximo permitido (2 MB).";
            default -> "La solicitud no es válida.";
        };
    }
}
