package com.example.mascotas.servicios;

import com.example.mascotas.entidades.Usuario;
import com.example.mascotas.entidades.Zona;
import com.example.mascotas.errores.ErrorServicio;
import com.example.mascotas.repositorios.UsuarioRespositorio;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServicioTest {

    private UsuarioServicio usuarioServicio;
    private UsuarioRespositorio usuarioRepositorio;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        usuarioServicio = new UsuarioServicio();
        usuarioRepositorio = Mockito.mock(UsuarioRespositorio.class);
        inyectar(usuarioServicio, "usuarioRepositorio", usuarioRepositorio);
    }

    @Test
    void validarAceptaDatosCorrectos() throws ErrorServicio {
        //Validando que no exista error al generar un usuario
        usuarioServicio.validar("Ana", "Pérez", "ana@example.com", "secreto1", "secreto1", new Zona());
    }

    @Test
    void validarRechazaUnaClaveCorta() {
        ErrorServicio error = assertThrows(ErrorServicio.class,
                () -> usuarioServicio.validar("Ana", "Pérez", "ana@example.com", "corta", "corta", new Zona()));

        //Validando que genere tal mensaje al momento de ingresar una password corta
        assertEquals("La calve del usuario no puede ser nula y tiene que tener mas de 6 digitos", error.getMessage());
    }

    @Test
    void loginDevuelveElUsuarioCuandoLasCredencialesSonCorrectas() throws ErrorServicio {
        Usuario usuario = usuarioActivo("ana@example.com", "secreto1");
        when(usuarioRepositorio.buscarPorMail("ana@example.com")).thenReturn(usuario);

        Usuario resultado = usuarioServicio.login("ana@example.com", "secreto1");

        assertSame(usuario, resultado);
        assertNull(resultado.getBaja());
    }

    @Test
    void deshabilitarMarcaAlUsuarioComoDadoDeBaja() throws ErrorServicio {
        Usuario usuario = usuarioActivo("ana@example.com", "secreto1");
        when(usuarioRepositorio.findById("usuario-1")).thenReturn(Optional.of(usuario));

        usuarioServicio.deshabilitar("usuario-1");

        assertTrue(usuario.getBaja() != null);
        verify(usuarioRepositorio).save(usuario);
    }

    @Test
    void habilitarQuitaLaFechaDeBaja() throws ErrorServicio {
        Usuario usuario = usuarioActivo("ana@example.com", "secreto1");
        usuario.setBaja(new java.util.Date());
        when(usuarioRepositorio.findById("usuario-1")).thenReturn(Optional.of(usuario));

        usuarioServicio.habilitar("usuario-1");

        assertFalse(usuario.getBaja() != null);
        verify(usuarioRepositorio).save(usuario);
    }

    @Test
    void deshabilitarInformaCuandoNoExisteElUsuario() {
        when(usuarioRepositorio.findById("inexistente")).thenReturn(Optional.empty());

        ErrorServicio error = assertThrows(ErrorServicio.class, () -> usuarioServicio.deshabilitar("inexistente"));

        assertEquals("No se encontro el usuario solicitado", error.getMessage());
        verify(usuarioRepositorio, never()).save(Mockito.any());
    }

    private Usuario usuarioActivo(String mail, String clave) {
        Usuario usuario = new Usuario();
        usuario.setMail(mail);
        usuario.setClave(clave);
        return usuario;
    }

    private void inyectar(Object destino, String nombreCampo, Object valor) throws ReflectiveOperationException {
        Field campo = destino.getClass().getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        campo.set(destino, valor);
    }
}
