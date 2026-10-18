package com.zero.ecommerce.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.repositories.UsuarioRepository;

@Service
public class UsuarioUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos."));

        if (usuario.isEliminado()) {
            throw new CuentaEliminadaException();
        }
        if (usuario.getCodigoActivacion() != null) {
            throw new ActivacionPendienteException();
        }
        if (usuario.getRol() == null || usuario.getClave() == null || usuario.getClave().isBlank()) {
            throw new UsernameNotFoundException("Usuario o contraseña incorrectos.");
        }

        return User.withUsername(usuario.getNombreUsuario())
                .password(usuario.getClave())
                .authorities("ROLE_" + usuario.getRol().name())
                .build();
    }
}
