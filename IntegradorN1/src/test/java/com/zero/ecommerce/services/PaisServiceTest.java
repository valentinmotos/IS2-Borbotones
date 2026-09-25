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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.PaisRepository;
import com.zero.ecommerce.repositories.ProvinciaRepository;

@ExtendWith(MockitoExtension.class)
class PaisServiceTest {

    @Mock
    private PaisRepository repository;
    @Mock
    private ProvinciaRepository provinciaRepository;
    @InjectMocks
    private PaisService service;

    @Test
    void rechazaDuplicadoSinImportarMayusculasNiTildes() {
        when(repository.findByEliminadoFalseOrderByNombreAsc()).thenReturn(List.of(pais("1", "Perú")));
        assertThatThrownBy(() -> service.crearPais("  PERU "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe un país con ese nombre.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void exigeNombre(String nombre) {
        assertThatThrownBy(() -> service.crearPais(nombre))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El nombre del país es obligatorio.");
        verify(repository, never()).save(any());
    }

    @Test
    void modificarConElMismoNombreNoEsDuplicado() throws Exception {
        Pais argentina = pais("1", "Argentina");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(argentina));
        when(repository.findByEliminadoFalseOrderByNombreAsc()).thenReturn(List.of(argentina));
        service.modificarPais("1", "argentina");
        assertThat(argentina.getNombre()).isEqualTo("argentina");
        verify(repository).save(argentina);
    }

    @Test
    void noEliminaUnPaisConProvinciasActivas() {
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(pais("1", "Argentina")));
        when(provinciaRepository.existsByPais_IdAndEliminadoFalse("1")).thenReturn(true);
        assertThatThrownBy(() -> service.eliminarPais("1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede eliminar el país porque tiene provincias activas.");
        verify(repository, never()).save(any());
    }

    @Test
    void eliminarEsBajaLogica() throws Exception {
        Pais chile = pais("2", "Chile");
        when(repository.findByIdAndEliminadoFalse("2")).thenReturn(Optional.of(chile));
        service.eliminarPais("2");
        assertThat(chile.isEliminado()).isTrue();
        verify(repository).save(chile);
    }

    private Pais pais(String id, String nombre) {
        Pais pais = new Pais();
        pais.setId(id);
        pais.setNombre(nombre);
        return pais;
    }
}
