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

import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.DireccionRepository;

@ExtendWith(MockitoExtension.class)
class DireccionServiceTest {

    @Mock
    private DireccionRepository repository;
    @Mock
    private LocalidadService localidadService;
    @InjectMocks
    private DireccionService service;

    @Test
    void creaLaDireccionConLosOpcionalesVaciosEnNull() throws Exception {
        Localidad maipu = new Localidad();
        when(localidadService.buscarLocalidad("maipu")).thenReturn(maipu);
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        Direccion direccion = service.crearDireccion(" San Martín ", " 1234 ", "  ", "", null, " Portón negro ",
                "maipu");
        assertThat(direccion.getCalle()).isEqualTo("San Martín");
        assertThat(direccion.getNumeracion()).isEqualTo("1234");
        assertThat(direccion.getBarrio()).isNull();
        assertThat(direccion.getManzanaPiso()).isNull();
        assertThat(direccion.getCasaDepartamento()).isNull();
        assertThat(direccion.getReferencia()).isEqualTo("Portón negro");
        assertThat(direccion.getLocalidad()).isSameAs(maipu);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void exigeCalle(String calle) {
        assertThatThrownBy(() -> service.crearDireccion(calle, "1234", null, null, null, null, "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La calle es obligatoria.");
        verify(repository, never()).save(any());
    }

    @Test
    void exigeNumeracion() {
        assertThatThrownBy(() -> service.crearDireccion("San Martín", " ", null, null, null, null, "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La numeración es obligatoria.");
    }

    @Test
    void exigeLocalidad() {
        assertThatThrownBy(() -> service.crearDireccion("San Martín", "1234", null, null, null, null, ""))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La localidad es obligatoria.");
    }

    @Test
    void rechazaUnaLocalidadEliminada() throws Exception {
        when(localidadService.buscarLocalidad("borrada"))
                .thenThrow(new ErrorServiceException("La localidad no existe o fue eliminada."));
        assertThatThrownBy(() -> service.crearDireccion("San Martín", "1234", null, null, null, null, "borrada"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La localidad no existe o fue eliminada.");
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaCamposDemasiadoLargos() {
        assertThatThrownBy(() -> service.crearDireccion("San Martín", "12345678901", null, null, null, null, "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La numeración no puede superar los 10 caracteres.");
        assertThatThrownBy(() -> service.crearDireccion("San Martín", "1234", null, null, null, "x".repeat(256),
                "maipu"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La referencia no puede superar los 255 caracteres.");
    }

    @Test
    void buscaPorCalleYNumeracionSinImportarMayusculasNiTildes() throws Exception {
        Direccion direccion = new Direccion();
        direccion.setCalle("San Martín");
        direccion.setNumeracion("1234");
        when(repository.findByNumeracionAndEliminadoFalse("1234")).thenReturn(List.of(direccion));
        assertThat(service.buscarDireccionPorCalleNumeracion("SAN MARTIN", "1234")).isSameAs(direccion);
    }

    @Test
    void eliminarEsBajaLogica() throws Exception {
        Direccion direccion = new Direccion();
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(direccion));
        service.eliminarDireccion("1");
        ArgumentCaptor<Direccion> guardada = ArgumentCaptor.forClass(Direccion.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().isEliminado()).isTrue();
    }
}
