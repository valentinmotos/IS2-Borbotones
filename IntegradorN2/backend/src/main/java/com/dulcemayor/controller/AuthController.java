package com.dulcemayor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.CambioClaveRequest;
import com.dulcemayor.dto.LoginRequest;
import com.dulcemayor.dto.LoginResponse;
import com.dulcemayor.dto.UsuarioSesionResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.AuthService;

import jakarta.validation.Valid;

/**
 * Login, cambio de clave, sesión actual y cierre de sesión (E1-01).
 * La identidad siempre sale del token (@AuthenticationPrincipal), nunca de la URL ni del cuerpo.
 * <pre>
 * POST /api/auth/login    público          200 · 400 · 401
 * PUT  /api/auth/clave    autenticado      200 (token nuevo) · 400 · 401
 * GET  /api/auth/yo       autenticado      200 · 401
 * POST /api/auth/logout   autenticado      204 · 401
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PutMapping("/clave")
    public LoginResponse modificarClave(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CambioClaveRequest request) {
        return authService.modificarClave(usuario.id(), request);
    }

    @GetMapping("/yo")
    public UsuarioSesionResponse yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.buscarSesion(usuario.id());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        authService.cerrarSesion(usuario.id());
        return ResponseEntity.noContent().build();
    }
}
