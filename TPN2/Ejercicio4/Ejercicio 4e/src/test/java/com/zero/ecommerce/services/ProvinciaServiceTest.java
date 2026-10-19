package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.DepartamentoRepository;
import com.zero.ecommerce.repositories.ProvinciaRepository;

@ExtendWith(MockitoExtension.class)
class ProvinciaServiceTest {

    @Mock
    private ProvinciaRepository repository;
    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private PaisService paisService;
    @InjectMocks
    private ProvinciaService service;

    @Test
    void rechazaDuplicadoDentroDelMismoPais() throws Exception {
        Pais argentina = pais("ar");
        when(paisService.buscarPais("ar")).thenReturn(argentina);
        when(repository.findByPais_IdAndEliminadoFalseOrderByNombreAsc("ar"))
                .thenReturn(List.of(provincia("1", "Córdoba", argentina)));
        assertThatThrownBy(() -> service.crearProvincia("cordoba", "ar"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una provincia con ese nombre en el país seleccionado.");
        verify(repository, never()).save(any());
    }

    @Test
    void permiteElMismoNombreEnOtroPais() throws Exception {
        Pais espana = pais("es");
        when(paisService.buscarPais("es")).thenReturn(espana);
        when(repository.findByPais_IdAndEliminadoFalseOrderByNombreAsc("es")).thenReturn(List.of());
        service.crearProvincia("Córdoba", "es");
        verify(repository).save(any());
    }

    @Test
    void exigePais() {
        assertThatThrownBy(() -> service.crearProvincia("Mendoza", ""))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El país es obligatorio.");
        verify(repository, never()).save(any());
    }

    @Test
    void noEliminaUnaProvinciaConDepartamentosActivos() {
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(provincia("1", "Mendoza", pais("ar"))));
        when(departamentoRepository.existsByProvincia_IdAndEliminadoFalse("1")).thenReturn(true);
        assertThatThrownBy(() -> service.eliminarProvincia("1"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede eliminar la provincia porque tiene departamentos activos.");
        verify(repository, never()).save(any());
    }

    private Pais pais(String id) {
        Pais pais = new Pais();
        pais.setId(id);
        return pais;
    }

    private Provincia provincia(String id, String nombre, Pais pais) {
        Provincia provincia = new Provincia();
        provincia.setId(id);
        provincia.setNombre(nombre);
        provincia.setPais(pais);
        return provincia;
    }
}
