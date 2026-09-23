package com.borbotones.integrador1.services;

import com.borbotones.integrador1.dto.CambioClaveForm;
import com.borbotones.integrador1.dto.UsuarioForm;
import com.borbotones.integrador1.entities.RolUsuario;
import com.borbotones.integrador1.entities.Usuario;
import com.borbotones.integrador1.repositories.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioForm crearFormularioUsuario() {
        return new UsuarioForm();
    }

    @Transactional(readOnly = true)
    public UsuarioForm crearFormularioEdicion(String id) {
        Usuario usuario = buscarUsuario(id);
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.getId());
        form.setNombreUsuario(usuario.getNombreUsuario());
        form.setRol(usuario.getRol());
        return form;
    }

    public CambioClaveForm crearFormularioCambioClave() {
        return new CambioClaveForm();
    }

    public RolUsuario[] listarRoles() {
        return RolUsuario.values();
    }

    @Transactional
    public Usuario crearUsuario(String nombreUsuario, String clave, RolUsuario rol) {
        validar(nombreUsuario, clave, rol);
        String nombreNormalizado = nombreUsuario.trim();
        validarNombreDisponible(nombreNormalizado, null);

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID().toString());
        usuario.setNombreUsuario(nombreNormalizado);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setRol(rol);
        usuario.setEliminado(false);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario modificarUsuario(String id, String nombreUsuario, String clave, RolUsuario rol) {
        validar(nombreUsuario, clave, rol);
        Usuario usuario = buscarUsuario(id);
        String nombreNormalizado = nombreUsuario.trim();
        validarNombreDisponible(nombreNormalizado, id);

        usuario.setNombreUsuario(nombreNormalizado);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    public void validar(String nombreUsuario, String clave, RolUsuario rol) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre de usuario");
        }
        if (nombreUsuario.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre de usuario no puede superar los 100 caracteres");
        }
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la clave");
        }
        if (clave.length() < 6 || clave.length() > 72) {
            throw new IllegalArgumentException("La clave debe tener entre 6 y 72 caracteres");
        }
        if (rol == null) {
            throw new IllegalArgumentException("Debe indicar el rol");
        }
    }

    @Transactional
    public void eliminarUsuario(String id) {
        Usuario usuario = buscarUsuario(id);
        usuario.setEliminado(true);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarUsuario(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encuentra el usuario indicado"));
        if (usuario.isEliminado()) {
            throw new IllegalArgumentException("No se encuentra el usuario indicado");
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario buscarUsuarioPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre de usuario");
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCaseAndEliminadoFalse(nombre.trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encuentra el usuario indicado"));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuario() {
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "nombreUsuario"));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarioActivo() {
        return usuarioRepository.findByEliminadoFalseOrderByNombreUsuarioAsc();
    }

    @Transactional
    public void modificarClave(String id, String claveActual, String nuevaClave, String confirmarClave) {
        Usuario usuario = buscarUsuario(id);
        if (claveActual == null || !passwordEncoder.matches(claveActual, usuario.getClave())) {
            throw new IllegalArgumentException("La clave actual es incorrecta");
        }
        if (nuevaClave == null || nuevaClave.length() < 6 || nuevaClave.length() > 72) {
            throw new IllegalArgumentException("La nueva clave debe tener entre 6 y 72 caracteres");
        }
        if (!nuevaClave.equals(confirmarClave)) {
            throw new IllegalArgumentException("La confirmacion de la clave no coincide");
        }
        usuario.setClave(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);
    }

    private void validarNombreDisponible(String nombreUsuario, String idActual) {
        usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario).ifPresent(existente -> {
            if (idActual == null || !existente.getId().equals(idActual)) {
                throw new IllegalArgumentException("Ya existe un usuario con el nombre indicado");
            }
        });
    }
}
