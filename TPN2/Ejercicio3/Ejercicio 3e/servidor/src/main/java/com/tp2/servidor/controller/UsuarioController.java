package com.tp2.servidor.controller;

import com.tp2.servidor.dto.UsuarioRequest;
import com.tp2.servidor.entity.Usuario;
import com.tp2.servidor.service.UsuarioService;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends BaseRestController<Usuario, UsuarioRequest, String> {
    public UsuarioController(UsuarioService usuarioService) {
        super(usuarioService);
    }

    @Override
    protected Optional<Usuario> modificarRecurso(String id, UsuarioRequest request) {
        return Optional.empty();
    }

    @Override
    protected void eliminarRecurso(String id) {
        // La API de usuarios no permite eliminar recursos.
    }
}
