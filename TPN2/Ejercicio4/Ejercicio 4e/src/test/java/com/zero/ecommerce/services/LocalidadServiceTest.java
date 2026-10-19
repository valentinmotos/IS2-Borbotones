package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.LocalidadRepository;

@ExtendWith(MockitoExtension.class)
class LocalidadServiceTest {

    @Mock
    private LocalidadRepository repository;
    @Mock
    private DepartamentoService departamentoService;
    @InjectMocks
    private LocalidadService service;

    @Test
    void rechazaDuplicadoDentroDelMismoDepartamento() throws Exception {
        Departamento maipu = departamento("maipu");
        when(departamentoService.buscarDepartamento("maipu")).thenReturn(maipu);
        when(repository.findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc("maipu"))
                .thenReturn(List.of(localidad("1", "Gutiérrez", maipu)));
        assertThatThrownBy(() -> service.crearLocalidad(" GUTIERREZ ", "5511", "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una localidad con ese nombre en el departamento seleccionado.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void exigeCodigoPostal(String codigoPostal) throws Exception {
        when(departamentoService.buscarDepartamento("maipu")).thenReturn(departamento("maipu"));
        assertThatThrownBy(() -> service.crearLocalidad("Russell", codigoPostal, "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El código postal de la localidad es obligatorio.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "55", "55000", "ABCD", "M55ABC" })
    void rechazaCodigoPostalConFormatoInvalido(String codigoPostal) throws Exception {
        when(departamentoService.buscarDepartamento("maipu")).thenReturn(departamento("maipu"));
        assertThatThrownBy(() -> service.crearLocalidad("Russell", codigoPostal, "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El código postal debe tener 4 dígitos (5500) o el formato CPA (M5500ABC).");
    }

    @Test
    void guardaElCpaEnMayusculas() throws Exception {
        Departamento maipu = departamento("maipu");
        when(departamentoService.buscarDepartamento("maipu")).thenReturn(maipu);
        service.crearLocalidad(" Russell ", "m5517abc", "maipu");
        ArgumentCaptor<Localidad> guardada = ArgumentCaptor.forClass(Localidad.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Russell");
        assertThat(guardada.getValue().getCodigoPostal()).isEqualTo("M5517ABC");
        assertThat(guardada.getValue().getDepartamento()).isSameAs(maipu);
    }

    @Test
    void exigeDepartamento() {
        assertThatThrownBy(() -> service.crearLocalidad("Russell", "5517", null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El departamento es obligatorio.");
        verify(repository, never()).save(any());
    }

    private Departamento departamento(String id) {
        Departamento departamento = new Departamento();
        departamento.setId(id);
        return departamento;
    }

    private Localidad localidad(String id, String nombre, Departamento departamento) {
        Localidad localidad = new Localidad();
        localidad.setId(id);
        localidad.setNombre(nombre);
        localidad.setDepartamento(departamento);
        return localidad;
    }
}
