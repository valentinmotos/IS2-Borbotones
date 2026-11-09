package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.entity.CotizacionDolar;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.CotizacionDolarRepository;
import com.dulcemayor.service.CotizacionProvider.CotizacionExterna;

/** E1-08: cotización vigente desde el proveedor y respaldo con la última guardada. */
@ExtendWith(MockitoExtension.class)
class CotizacionDolarServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    @Mock
    private CotizacionDolarRepository repository;

    @Mock
    private CotizacionProvider provider;

    private CotizacionDolarService service;

    @BeforeEach
    void setUp() {
        service = new CotizacionDolarService(repository, provider);
    }

    private static CotizacionDolar guardada(LocalDate fecha, double valor) {
        CotizacionDolar cotizacion = new CotizacionDolar();
        cotizacion.setFecha(fecha);
        cotizacion.setValorVenta(valor);
        return cotizacion;
    }

    @Test
    void conLaApiDisponibleGuardaYDevuelveLaCotizacionDelDia() {
        when(provider.obtenerVigente()).thenReturn(Optional.of(new CotizacionExterna(HOY, 1450)));
        when(repository.findByFecha(HOY)).thenReturn(Optional.empty());
        when(repository.save(any(CotizacionDolar.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CotizacionDolarResponse vigente = service.buscarCotizacionVigente();

        assertThat(vigente).isEqualTo(new CotizacionDolarResponse(HOY, 1450));
        verify(repository).save(any(CotizacionDolar.class));
    }

    @Test
    void siYaHayCotizacionDelDiaSeActualizaLaMismaFila() {
        CotizacionDolar existente = guardada(HOY, 1400);
        when(provider.obtenerVigente()).thenReturn(Optional.of(new CotizacionExterna(HOY, 1455)));
        when(repository.findByFecha(HOY)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        assertThat(service.actualizarCotizacion().valorVenta()).isEqualTo(1455);
        assertThat(existente.getValorVenta()).isEqualTo(1455);
    }

    @Test
    void siLaApiFallaDevuelveLaUltimaGuardada() {
        when(provider.obtenerVigente()).thenReturn(Optional.empty());
        when(repository.findFirstByOrderByFechaDesc()).thenReturn(Optional.of(guardada(HOY.minusDays(1), 1430)));

        CotizacionDolarResponse vigente = service.buscarCotizacionVigente();

        assertThat(vigente).isEqualTo(new CotizacionDolarResponse(HOY.minusDays(1), 1430));
        verify(repository, never()).save(any());
    }

    @Test
    void siLaApiFallaYNoHayNadaGuardadoDevuelve404() {
        when(provider.obtenerVigente()).thenReturn(Optional.empty());
        when(repository.findFirstByOrderByFechaDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarCotizacionVigente())
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
