package com.tp2.servidor.service;

import com.tp2.servidor.dao.UsuarioDao;
import com.tp2.servidor.dto.UsuarioRequest;
import com.tp2.servidor.entity.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService extends BaseRestService<Usuario, UsuarioRequest, String> {
    private final UsuarioDao usuarioDao;

    public UsuarioService(UsuarioDao usuarioDao) {
        super(usuarioDao, UsuarioService::convertir, usuario -> !usuario.isEliminado());
        this.usuarioDao = usuarioDao;
    }

    private static Usuario convertir(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setClave(request.getClave());
        return usuario;
    }

    @Override
    protected void marcarEliminada(Usuario usuario) {
        usuario.setEliminado(true);
    }

    public Usuario buscarActivo(String id) {
        return buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o eliminado"));
    }
}
