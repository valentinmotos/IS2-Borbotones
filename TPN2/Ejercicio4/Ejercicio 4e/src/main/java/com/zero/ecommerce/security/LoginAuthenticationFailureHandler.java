package com.zero.ecommerce.security;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
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
        // DaoAuthenticationProvider envuelve en InternalAuthenticationServiceException lo que lanza
        // UsuarioUserDetailsService (salvo UsernameNotFoundException): el motivo real está en la causa.
        Throwable error = exception instanceof InternalAuthenticationServiceException && exception.getCause() != null
                ? exception.getCause() : exception;
        String motivo = "credenciales";
        if (error instanceof CuentaEliminadaException) {
            motivo = "eliminada";
        } else if (error instanceof ActivacionPendienteException) {
            motivo = "activacion";
        } else if (error instanceof DisabledException) {
            motivo = "deshabilitada";
        }
        response.sendRedirect(request.getContextPath() + "/login?error=" + motivo);
    }
}
