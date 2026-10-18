package com.zero.ecommerce.security;

import org.springframework.security.authentication.DisabledException;

public class CuentaEliminadaException extends DisabledException {

    private static final long serialVersionUID = 1L;

    public CuentaEliminadaException() {
        super("La cuenta fue dada de baja.");
    }
}
