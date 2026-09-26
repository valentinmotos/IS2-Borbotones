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
    void unJefeNoPuedeDarseDeBajaASiMismo() {
        when(usuarioRepository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(usuario("1", RolUsuario.JEFE)));
        assertThatThrownBy(() -> service.eliminarUsuario("1", "1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No podés darte de baja a vos mismo.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noPermiteDarDeBajaAlUltimoJefe() {
        when(usuarioRepository.findByIdAndEliminadoFalse("2")).thenReturn(Optional.of(usuario("2", RolUsuario.JEFE)));
        when(usuarioRepository.countByRolAndEliminadoFalse(RolUsuario.JEFE)).thenReturn(1L);
        assertThatThrownBy(() -> service.eliminarUsuario("2", "1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Debe quedar al menos un JEFE activo en el sistema.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void permiteDarDeBajaUnJefeSiQuedaOtro() throws Exception {
        Usuario jefe = usuario("2", RolUsuario.JEFE);
        when(usuarioRepository.findByIdAndEliminadoFalse("2")).thenReturn(Optional.of(jefe));
        when(usuarioRepository.countByRolAndEliminadoFalse(RolUsuario.JEFE)).thenReturn(2L);
        service.eliminarUsuario("2", "1");
        assertThat(jefe.isEliminado()).isTrue();
        verify(usuarioRepository).save(jefe);
    }

    @Test
    void noPermiteQuitarleElRolAlUltimoJefe() {
        when(usuarioRepository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(usuario("1", RolUsuario.JEFE)));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("jefe@zero.com.ar")).thenReturn(Optional.empty());
        when(usuarioRepository.countByRolAndEliminadoFalse(RolUsuario.JEFE)).thenReturn(1L);
        assertThatThrownBy(() -> service.modificarUsuarioEmpleado("1", "jefe@zero.com.ar", RolUsuario.ADMINISTRATIVO,
                null, null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Debe quedar al menos un JEFE activo en el sistema.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void creaEmpleadoActivoConClaveEncriptada() throws Exception {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("nuevo@zero.com.ar")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave123!")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        Usuario creado = service.crearUsuarioEmpleado(" Nuevo@Zero.com.ar ", RolUsuario.ADMINISTRATIVO, "Clave123!",
                "Clave123!");
        assertThat(creado.getNombreUsuario()).isEqualTo("nuevo@zero.com.ar");
        assertThat(creado.getClave()).isEqualTo("hash");
        assertThat(creado.getCodigoActivacion()).isNull();
    }

    @Test
    void rechazaCorreoRepetidoAlCrearEmpleado() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("admin@zero.com.ar"))
                .thenReturn(Optional.of(usuario("9", RolUsuario.ADMINISTRATIVO)));
        assertThatThrownBy(() -> service.crearUsuarioEmpleado("admin@zero.com.ar", RolUsuario.ADMINISTRATIVO,
                "Clave123!", "Clave123!"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una cuenta registrada con ese correo.");
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuario(String id, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombreUsuario("usuario" + id + "@zero.com.ar");
        usuario.setRol(rol);
        return usuario;
    }
}
