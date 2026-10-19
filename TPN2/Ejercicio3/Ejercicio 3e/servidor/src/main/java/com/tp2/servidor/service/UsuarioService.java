package com.tp2.servidor.service;

import com.tp2.servidor.dao.UsuarioDao;
import com.tp2.servidor.dto.UsuarioRequest;
import com.tp2.servidor.entity.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService extends BaseRestService<Usuario, UsuarioRequest, String> {
    public UsuarioService(UsuarioDao usuarioDao) {
        super(usuarioDao, UsuarioService::convertir, usuario -> !usuario.isEliminado());
    }

    private static Usuario convertir(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setClave(request.getClave());
        return usuario;
    }

    @Override
    protected void validar(Operacion operacion, Usuario usuario) {
        if (operacion != Operacion.BAJA && (usuario.getNombre() == null || usuario.getNombre().isBlank())) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }
    }

    @Override
    protected void actualizar(Usuario existente, Usuario nuevo) {
        existente.setNombre(nuevo.getNombre());
        if (nuevo.getClave() != null && !nuevo.getClave().isBlank()) {
            existente.setClave(nuevo.getClave());
        }
    }

    @Override
    protected void marcarEliminada(Usuario usuario) {
        usuario.setEliminado(true);
    }
}
