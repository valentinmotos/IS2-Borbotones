package com.zero.ecommerce.services;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Persona;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.PersonaRepository;
import com.zero.ecommerce.repositories.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PersonaRepository personaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
    }

    public Usuario buscarUsuario(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El usuario no existe o fue eliminado.");
        }
        return usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El usuario no existe o fue eliminado."));
    }

    public Usuario buscarUsuarioPorNombre(String nombreUsuario) throws ErrorServiceException {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new ErrorServiceException("El usuario no existe o fue eliminado.");
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCaseAndEliminadoFalse(nombreUsuario.strip())
                .orElseThrow(() -> new ErrorServiceException("El usuario no existe o fue eliminado."));
    }

    public Optional<Usuario> usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCaseAndEliminadoFalse(authentication.getName());
    }

    public String nombreParaMostrar(Usuario usuario) {
        if (usuario == null) {
            return "";
        }
        Optional<Persona> persona = personaRepository
                .findFirstByUsuario_NombreUsuarioIgnoreCaseAndEliminadoFalse(usuario.getNombreUsuario());
        if (persona.isEmpty()) {
            return usuario.getNombreUsuario();
        }

        String nombre = persona.get().getNombre() == null ? "" : persona.get().getNombre().strip();
        String apellido = persona.get().getApellido() == null ? "" : persona.get().getApellido().strip();
        String nombreCompleto = (nombre + " " + apellido).strip();
        return nombreCompleto.isBlank() ? usuario.getNombreUsuario() : nombreCompleto;
    }
}
