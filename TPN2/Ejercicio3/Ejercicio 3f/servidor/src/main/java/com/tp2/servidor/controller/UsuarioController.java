package com.tp2.servidor.controller;

import com.tp2.servidor.dto.UsuarioRequest;
import com.tp2.servidor.entity.Usuario;
import com.tp2.servidor.service.UsuarioService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends BaseRestController<Usuario, UsuarioRequest, String> {
    public UsuarioController(UsuarioService service) {
        super(service);
    }
}
