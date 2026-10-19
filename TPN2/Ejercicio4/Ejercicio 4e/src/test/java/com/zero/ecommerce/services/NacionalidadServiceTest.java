package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.NacionalidadRepository;

@ExtendWith(MockitoExtension.class)
class NacionalidadServiceTest {

    @Mock
    private NacionalidadRepository repository;
    @InjectMocks
    private NacionalidadService service;

    @Test
    void rechazaDuplicadoInclusoConMayusculasUnicodeYEspacios() {
        when(repository.findAllByOrderByNombreAsc()).thenReturn(List.of(nacionalidad("1", "España")));
        assertThatThrownBy(() -> service.crearNacionalidad("  ESPAÑA  "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una nacionalidad con ese nombre.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t\n" })
    void exigeNombre(String nombre) {
        assertThatThrownBy(() -> service.crearNacionalidad(nombre))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El nombre de la nacionalidad es obligatorio.");
        verify(repository, never()).save(any());
    }

    @Test
    void creaConNombreSinEspaciosEnLosExtremos() throws Exception {
        service.crearNacionalidad(" Argentina ");
        ArgumentCaptor<Nacionalidad> guardada = ArgumentCaptor.forClass(Nacionalidad.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Argentina");
        assertThat(guardada.getValue().isEliminado()).isFalse();
    }

    @Test
    void permiteEditarSinCambiarElNombre() throws Exception {
        Nacionalidad actual = nacionalidad("1", "Argentina");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        when(repository.findAllByOrderByNombreAsc()).thenReturn(List.of(actual));
        service.modificarNacionalidad("1", " Argentina ");
        verify(repository).save(actual);
    }

    @Test
    void rechazaEditarConNombreDeOtraSinModificarLaEntidad() {
        Nacionalidad actual = nacionalidad("1", "Argentina");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        when(repository.findAllByOrderByNombreAsc()).thenReturn(List.of(actual, nacionalidad("2", "Chile")));
        assertThatThrownBy(() -> service.modificarNacionalidad("1", "Chile"))
                .isInstanceOf(ErrorServiceException.class);
        assertThat(actual.getNombre()).isEqualTo("Argentina");
        verify(repository, never()).save(any());
    }

    @Test
    void conservaLosNombresDeLasNacionalidadesEliminadas() {
        Nacionalidad eliminada = nacionalidad("1", "Argentina");
        eliminada.setEliminado(true);
        when(repository.findAllByOrderByNombreAsc()).thenReturn(List.of(eliminada));
        assertThatThrownBy(() -> service.crearNacionalidad("Argentina"))
                .isInstanceOf(ErrorServiceException.class);
    }

    @Test
    void eliminaLogicamente() throws Exception {
        Nacionalidad actual = nacionalidad("1", "Argentina");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        service.eliminarNacionalidad("1");
        assertThat(actual.isEliminado()).isTrue();
        verify(repository).save(actual);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void rechazaModificarIdInexistente() {
        assertThatThrownBy(() -> service.modificarNacionalidad("inexistente", "Argentina"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La nacionalidad no existe o fue eliminada.");
        verify(repository, never()).save(any());
    }

    private Nacionalidad nacionalidad(String id, String nombre) {
        Nacionalidad nacionalidad = new Nacionalidad();
        nacionalidad.setId(id);
        nacionalidad.setNombre(nombre);
        return nacionalidad;
    }
}
