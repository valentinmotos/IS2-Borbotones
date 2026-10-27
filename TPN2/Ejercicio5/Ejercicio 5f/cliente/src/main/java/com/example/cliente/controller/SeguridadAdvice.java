package com.example.cliente.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class SeguridadAdvice {
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public String sesionVencida(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login?vencida";
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public String sinPermisos() {
        return "acceso-denegado";
    }
}
