package com.dulcemayor.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.dulcemayor.dto.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde los rechazos de Spring Security con el mismo JSON {mensaje, campo} que el resto de la API:
 * 401 cuando no hay sesión (token ausente o inválido) y 403 cuando el rol no alcanza.
 * Lo registra SecurityConfig; E1-01 lo sigue usando con el filtro JWT.
 */
@Component
public class ErroresSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ErroresSeguridadHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "Tenés que iniciar sesión para acceder a este recurso.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción.");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorRespuesta.de(mensaje));
    }
}
