package com.borbotones.integrador1.services;

import com.borbotones.integrador1.dto.UsuarioForm;
import com.borbotones.integrador1.entities.RolUsuario;
import com.borbotones.integrador1.entities.Usuario;
import com.borbotones.integrador1.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void crearUsuarioCodificaLaClave() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("maxi")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = usuarioService.crearUsuario(" maxi ", "secreto", RolUsuario.ADMINISTRATIVO);

        assertTrue(passwordEncoder.matches("secreto", usuario.getClave()));
        assertFalse(usuario.isEliminado());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void crearUsuarioRechazaNombreDuplicado() {
        Usuario existente = usuario("1", "maxi", "secreto");
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("maxi")).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.crearUsuario("maxi", "otraClave", RolUsuario.CLIENTE));
    }

    @Test
    void crearFormularioEdicionMapeaLosDatosDelUsuario() {
        Usuario usuario = usuario("1", "maxi", "secreto");
        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));

        UsuarioForm form = usuarioService.crearFormularioEdicion("1");

        assertEquals("1", form.getId());
        assertEquals("maxi", form.getNombreUsuario());
        assertEquals(RolUsuario.CLIENTE, form.getRol());
    }

    @Test
    void modificarUsuarioConservaLaClaveActual() {
        Usuario usuario = usuario("1", "maxi", "secreto");
        String claveCodificada = usuario.getClave();
        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("maxi-editado")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario modificado = usuarioService.modificarUsuario("1", "maxi-editado", RolUsuario.ADMINISTRATIVO);

        assertEquals(claveCodificada, modificado.getClave());
        assertEquals("maxi-editado", modificado.getNombreUsuario());
        assertEquals(RolUsuario.ADMINISTRATIVO, modificado.getRol());
    }

    @Test
    void eliminarUsuarioRealizaBajaLogica() {
        Usuario usuario = usuario("1", "maxi", "secreto");
        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));

        usuarioService.eliminarUsuario("1");

        assertTrue(usuario.isEliminado());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void listarUsuarioActivoUsaLaConsultaQueExcluyeEliminados() {
        when(usuarioRepository.findByEliminadoFalseOrderByNombreUsuarioAsc()).thenReturn(List.of());

        usuarioService.listarUsuarioActivo();

        verify(usuarioRepository).findByEliminadoFalseOrderByNombreUsuarioAsc();
    }

    @Test
    void buscarUsuariosActivosFiltraPorParteDelNombre() {
        when(usuarioRepository.findByNombreUsuarioContainingIgnoreCaseAndEliminadoFalseOrderByNombreUsuarioAsc("max"))
                .thenReturn(List.of());

        usuarioService.buscarUsuariosActivos(" max ");

        verify(usuarioRepository)
                .findByNombreUsuarioContainingIgnoreCaseAndEliminadoFalseOrderByNombreUsuarioAsc("max");
    }

    @Test
    void modificarClaveValidaLaActualYCodificaLaNueva() {
        Usuario usuario = usuario("1", "maxi", "secreto");
        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));

        usuarioService.modificarClave("1", "secreto", "nuevoSecreto", "nuevoSecreto");

        assertTrue(passwordEncoder.matches("nuevoSecreto", usuario.getClave()));
        verify(usuarioRepository).save(usuario);
    }

    private Usuario usuario(String id, String nombre, String clave) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombreUsuario(nombre);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setEliminado(false);
        return usuario;
    }
}
