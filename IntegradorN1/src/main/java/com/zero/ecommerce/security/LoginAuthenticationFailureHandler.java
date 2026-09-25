package com.zero.ecommerce.security;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String motivo = "credenciales";
        if (exception instanceof CuentaEliminadaException) {
            motivo = "eliminada";
        } else if (exception instanceof ActivacionPendienteException) {
            motivo = "activacion";
        } else if (exception instanceof DisabledException) {
            motivo = "deshabilitada";
        }
        response.sendRedirect(request.getContextPath() + "/login?error=" + motivo);
    }
}
