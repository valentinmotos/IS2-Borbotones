package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.dto.RegistroForm;
import com.borbotones.videojuegos.entities.Rol;
import com.borbotones.videojuegos.entities.Usuario;
import com.borbotones.videojuegos.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(RegistroForm form) {
        if (form == null || form.getEmail() == null || form.getNombre() == null || form.getPassword() == null) {
            throw new IllegalArgumentException("Los datos de registro son obligatorios");
        }
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        String nombre = form.getNombre().trim();
        if (!nombre.matches("^[\\p{L}][\\p{L} '-]{1,59}$")) {
            throw new IllegalArgumentException("El nombre contiene caracteres no permitidos");
        }
        if (email.length() > 254 || !email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("El email no es valido");
        }
        if (!form.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$")) {
            throw new IllegalArgumentException("La contrasena no cumple los requisitos de seguridad");
        }
        if (!Objects.equals(form.getPassword(), form.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(form.getPassword()));
        usuario.setRol(Rol.USUARIO);
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }
}
