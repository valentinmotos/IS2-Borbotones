package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.PersonaRepository;
import com.zero.ecommerce.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private UsuarioService service;

    @Test
    void modificarClaveRechazaClaveActualIncorrecta() {
        when(usuarioRepository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(cliente()));
        when(passwordEncoder.matches("Incorrecta1", "hash-actual")).thenReturn(false);
        assertThatThrownBy(() -> service.modificarClave("1", "Incorrecta1", "Nueva1234", "Nueva1234"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La clave actual no es correcta.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void modificarClaveAplicaLasReglasDelRegistro() {
        when(usuarioRepository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(cliente()));
        when(passwordEncoder.matches("Cliente123!", "hash-actual")).thenReturn(true);
        assertThatThrownBy(() -> service.modificarClave("1", "Cliente123!", "corta", "corta"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La clave tiene que tener al menos " + UsuarioService.LARGO_MINIMO_CLAVE + " caracteres.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void modificarClaveGuardaLaNuevaClaveEncriptada() throws Exception {
        Usuario usuario = cliente();
        when(usuarioRepository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Cliente123!", "hash-actual")).thenReturn(true);
        when(passwordEncoder.encode("Nueva1234")).thenReturn("hash-nuevo");
        service.modificarClave("1", "Cliente123!", "Nueva1234", "Nueva1234");
        assertThat(usuario.getClave()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(usuario);
    }

    private Usuario cliente() {
        Usuario usuario = new Usuario();
        usuario.setId("1");
        usuario.setNombreUsuario("cliente@zero.com.ar");
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setClave("hash-actual");
        return usuario;
    }
}
