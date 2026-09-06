package com.example.mascotas.configuracion;

import com.example.mascotas.entidades.Usuario;
import com.example.mascotas.repositorios.UsuarioRespositorio;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetallesServicio implements UserDetailsService {
    private final UsuarioRespositorio usuarioRepositorio;
    public UsuarioDetallesServicio(UsuarioRespositorio usuarioRepositorio) { this.usuarioRepositorio = usuarioRepositorio; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.buscarPorMail(email == null ? "" : email.trim().toLowerCase());
        if (usuario == null || usuario.getBaja() != null) throw new UsernameNotFoundException("Credenciales incorrectas");
        return User.withUsername(usuario.getMail()).password(usuario.getClave()).authorities("ROLE_USER").build();
    }
}
