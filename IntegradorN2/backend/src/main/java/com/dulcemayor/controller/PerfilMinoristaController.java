package com.dulcemayor.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.PerfilMinoristaRequest;
import com.dulcemayor.dto.PerfilMinoristaResponse;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.MinoristaService;

import jakarta.validation.Valid;

/**
 * E2-07: perfil del minorista autenticado. Solo MINORISTA (regla de /api/minorista/** en
 * SecurityConfig). No recibe ids: el minorista sale del token, así nadie puede ver ni editar el
 * perfil de otro. El cambio de clave voluntario usa PUT /api/auth/clave (E1-01).
 */
@RestController
@RequestMapping("/api/minorista/perfil")
public class PerfilMinoristaController {

    private final MinoristaService minoristaService;

    public PerfilMinoristaController(MinoristaService minoristaService) {
        this.minoristaService = minoristaService;
    }

    @GetMapping
    public PerfilMinoristaResponse buscar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return minoristaService.buscarPerfil(usuario.id());
    }

    @PutMapping
    public PerfilMinoristaResponse modificar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PerfilMinoristaRequest request) {
        return minoristaService.modificarMinorista(usuario.id(), request);
    }
}
