package com.zero.ecommerce.security;

import org.springframework.security.authentication.DisabledException;

public class ActivacionPendienteException extends DisabledException {

    private static final long serialVersionUID = 1L;

    public ActivacionPendienteException() {
        super("La cuenta todavía no está activada. Revisá tu correo para completar la activación.");
    }
}
