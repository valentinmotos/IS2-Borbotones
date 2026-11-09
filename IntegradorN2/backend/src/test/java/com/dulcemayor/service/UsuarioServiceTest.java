package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.NoAutenticadoException;
import com.dulcemayor.repository.UsuarioRepository;

/** E1-01: BCrypt, clave por defecto temporal, login y cambio de clave. */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    /** BCrypt real con costo bajo para que los tests sean rápidos. */
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, encoder);
    }

    private Usuario usuario(String id, String clave, boolean temporal) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombreUsuario("preventor@dulcemayor.com.ar");
        usuario.setRol(RolUsuario.PREVENTOR);
        usuario.setClave(encoder.encode(clave));
        usuario.setClaveTemporal(temporal);
        return usuario;
    }

    @Test
    void crearUsuarioUsaLaClavePorDefectoEncriptadaYTemporal() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario creado = service.crearUsuario("  Preventor@DulceMayor.com.ar ", RolUsuario.PREVENTOR);

        assertThat(creado.getNombreUsuario()).isEqualTo("preventor@dulcemayor.com.ar");
        assertThat(creado.isClaveTemporal()).isTrue();
        assertThat(creado.getClave()).isNotEqualTo(UsuarioService.CLAVE_POR_DEFECTO).startsWith("$2a$");
        assertThat(encoder.matches(UsuarioService.CLAVE_POR_DEFECTO, creado.getClave())).isTrue();
    }

    @Test
    void crearUsuarioConCorreoRepetidoDevuelveDuplicado() {
        when(usuarioRepository.existsByNombreUsuario("jefe@dulcemayor.com.ar")).thenReturn(true);

        assertThatThrownBy(() -> service.crearUsuario("JEFE@dulcemayor.com.ar", RolUsuario.JEFE))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("Ya existe una cuenta registrada con ese correo.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearUsuarioConCorreoInvalidoOClaveCortaSeRechaza() {
        assertThatThrownBy(() -> service.crearUsuario("no-es-correo", RolUsuario.JEFE))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El correo electrónico no tiene un formato válido.");
        assertThatThrownBy(() -> service.crearUsuario("a@b.com", RolUsuario.JEFE, "corta", false))
                .hasMessage("La clave tiene que tener al menos 8 caracteres.");
    }

    @Test
    void loginCorrectoDevuelveElUsuario() {
        Usuario guardado = usuario("id-1", "dulcemayor", true);
        when(usuarioRepository.findByNombreUsuarioAndEliminadoFalse("preventor@dulcemayor.com.ar"))
                .thenReturn(Optional.of(guardado));

        Usuario logueado = service.login(" PREVENTOR@dulcemayor.com.ar", "dulcemayor");

        assertThat(logueado).isSameAs(guardado);
        assertThat(logueado.isClaveTemporal()).isTrue();
    }

    @Test
    void loginConClaveIncorrectaOCorreoInexistenteDaElMismoMensaje() {
        when(usuarioRepository.findByNombreUsuarioAndEliminadoFalse("preventor@dulcemayor.com.ar"))
                .thenReturn(Optional.of(usuario("id-1", "dulcemayor", true)));
        when(usuarioRepository.findByNombreUsuarioAndEliminadoFalse("nadie@dulcemayor.com.ar"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("preventor@dulcemayor.com.ar", "otraclave"))
                .isInstanceOf(NoAutenticadoException.class).hasMessage("Correo o clave incorrectos.");
        assertThatThrownBy(() -> service.login("nadie@dulcemayor.com.ar", "dulcemayor"))
                .isInstanceOf(NoAutenticadoException.class).hasMessage("Correo o clave incorrectos.");
    }

    @Test
    void loginDeCuentaSinActivarSeRechaza() {
        Usuario pendiente = usuario("id-1", "Clave1234", false);
        pendiente.setActivo(false);
        when(usuarioRepository.findByNombreUsuarioAndEliminadoFalse(any())).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> service.login("preventor@dulcemayor.com.ar", "Clave1234"))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("todavía no está activada");
    }

    @Test
    void modificarClaveQuitaLaMarcaTemporalEInvalidaTokensAnteriores() {
        Usuario guardado = usuario("id-1", "dulcemayor", true);
        when(usuarioRepository.findByIdAndEliminadoFalse("id-1")).thenReturn(Optional.of(guardado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario modificado = service.modificarClave("id-1", "dulcemayor", "NuevaClave1", "NuevaClave1");

        assertThat(modificado.isClaveTemporal()).isFalse();
        assertThat(modificado.getVersionToken()).isEqualTo(1);
        assertThat(encoder.matches("NuevaClave1", modificado.getClave())).isTrue();
    }

    @Test
    void modificarClaveExigeLaActualCorrectaUnaDistintaYConfirmacion() {
        when(usuarioRepository.findByIdAndEliminadoFalse("id-1"))
                .thenReturn(Optional.of(usuario("id-1", "dulcemayor", true)));

        assertThatThrownBy(() -> service.modificarClave("id-1", "equivocada", "NuevaClave1", "NuevaClave1"))
                .hasMessage("La clave actual no es correcta.");
        assertThatThrownBy(() -> service.modificarClave("id-1", "dulcemayor", "NuevaClave1", "Otra12345"))
                .hasMessage("La confirmación no coincide con la clave nueva.");
        assertThatThrownBy(() -> service.modificarClave("id-1", "dulcemayor", "dulcemayor", "dulcemayor"))
                .hasMessage("La clave nueva tiene que ser distinta de la actual.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cerrarSesionIncrementaLaVersionDelToken() {
        Usuario guardado = usuario("id-1", "Clave1234", false);
        when(usuarioRepository.findByIdAndEliminadoFalse("id-1")).thenReturn(Optional.of(guardado));

        service.cerrarSesion("id-1");

        assertThat(guardado.getVersionToken()).isEqualTo(1);
        verify(usuarioRepository).save(guardado);
    }
}
