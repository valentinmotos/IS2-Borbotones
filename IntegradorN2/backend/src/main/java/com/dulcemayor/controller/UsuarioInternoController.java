package com.dulcemayor.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.dulcemayor.dto.UsuarioInternoRequest;
import com.dulcemayor.dto.UsuarioInternoResponse;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.security.UsuarioAutenticado;
import com.dulcemayor.service.UsuarioInternoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios/internos")
public class UsuarioInternoController extends AbstractCrudController<UsuarioInternoRequest, UsuarioInternoResponse> {

    private final UsuarioInternoService usuarioInternoService;

    public UsuarioInternoController(UsuarioInternoService servicio) {
        super(servicio);
        this.usuarioInternoService = servicio;
    }

    @Override
    @GetMapping
    public List<UsuarioInternoResponse> listar() {
        return usuarioInternoService.listarUsuariosInternos();
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioInternoResponse crear(@Valid @RequestBody UsuarioInternoRequest request) {
        return usuarioInternoService.crearUsuarioInterno(request);
    }

    @Override
    @PutMapping("/{id}")
    public UsuarioInternoResponse modificar(@PathVariable String id,
            @Valid @RequestBody UsuarioInternoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        if (actual != null && actual.id().equals(id)) {
            throw new ErrorServiceException("No podés modificar tu propia cuenta desde esta pantalla.");
        }
        return usuarioInternoService.modificarUsuarioInterno(id, request);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        if (actual != null && actual.id().equals(id)) {
            throw new ErrorServiceException("No podés dar de baja tu propia cuenta.");
        }
        usuarioInternoService.eliminarUsuarioInterno(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resetear-clave")
    @ResponseStatus(HttpStatus.OK)
    public UsuarioInternoResponse resetearClave(@PathVariable String id,
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        if (actual != null && actual.id().equals(id)) {
            throw new ErrorServiceException("No podés resetear tu propia clave desde esta pantalla.");
        }
        return usuarioInternoService.resetearClave(id);
    }
}
