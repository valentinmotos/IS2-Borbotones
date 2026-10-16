package com.tp2.cliente.service;

import com.tp2.cliente.dao.UsuarioDao;
import com.tp2.cliente.dto.UsuarioDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioDao usuarioDao;

    public UsuarioService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public List<UsuarioDto> listar() {
        return usuarioDao.listar();
    }
}
