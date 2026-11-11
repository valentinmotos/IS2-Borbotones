package com.dulcemayor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.UsuarioInternoRequest;
import com.dulcemayor.dto.UsuarioInternoResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.UsuarioInternoService;

/**
 * E2-05: ABM de usuarios internos (solo JEFE, regla en SecurityConfig). Extiende el
 * AbstractCrudController de E0-08 y redefine sus métodos con la MISMA firma, así las rutas
 * GET/POST/PUT/DELETE se heredan sin duplicarse. Las reglas sobre la propia cuenta (no editarse,
 * no darse de baja, no resetearse la clave) las valida el service con el usuario del token.
 */
@RestController
@RequestMapping("/api/usuarios/internos")
public class UsuarioInternoController extends AbstractCrudController<UsuarioInternoRequest, UsuarioInternoResponse> {

    private final UsuarioInternoService usuarioInternoService;

    public UsuarioInternoController(UsuarioInternoService servicio) {
        super(servicio);
        this.usuarioInternoService = servicio;
    }

    @Override
    public List<UsuarioInternoResponse> listar() {
        return usuarioInternoService.listarUsuariosInternos();
    }

    @Override
    public UsuarioInternoResponse crear(UsuarioInternoRequest request) {
        return usuarioInternoService.crearUsuarioInterno(request);
    }

    @Override
    public UsuarioInternoResponse modificar(String id, UsuarioInternoRequest request) {
        return usuarioInternoService.modificarUsuarioInterno(id, request, idUsuarioActual());
    }

    @Override
    public ResponseEntity<Void> eliminar(String id) {
        usuarioInternoService.eliminarUsuarioInterno(id, idUsuarioActual());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resetear-clave")
    @ResponseStatus(HttpStatus.OK)
    public UsuarioInternoResponse resetearClave(@PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        return usuarioInternoService.resetearClave(id, actual == null ? null : actual.id());
    }

    /** Id del usuario del token (los métodos heredados no pueden sumar el parámetro del principal). */
    private static String idUsuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
                ? usuario.id()
                : null;
    }
}
