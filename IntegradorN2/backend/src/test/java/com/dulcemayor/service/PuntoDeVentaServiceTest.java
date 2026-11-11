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

import com.dulcemayor.dto.PuntoDeVentaRequest;
import com.dulcemayor.dto.mapper.PerfilMinoristaMapperImpl;
import com.dulcemayor.dto.mapper.PuntoDeVentaMapperImpl;
import com.dulcemayor.entity.Departamento;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Localidad;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Preventor;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DireccionRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.PreventorRepository;
import com.dulcemayor.repository.PuntoDeVentaRepository;

@ExtendWith(MockitoExtension.class)
class PuntoDeVentaServiceTest {

    @Mock private PuntoDeVentaRepository puntoRepository;
    @Mock private MinoristaRepository minoristaRepository;
    @Mock private PreventorRepository preventorRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private LocalidadService localidadService;
    private PuntoDeVentaService service;

    @BeforeEach
    void setUp() {
        DireccionService direccionService = new DireccionService(direccionRepository, localidadService);
        service = new PuntoDeVentaService(puntoRepository, new PuntoDeVentaMapperImpl(),
                new MinoristaService(minoristaRepository, direccionService, new PerfilMinoristaMapperImpl()),
                new PreventorService(preventorRepository), direccionService);
    }

    private static PuntoDeVentaRequest request(String minoristaId, Double latitud, Double longitud) {
        return new PuntoDeVentaRequest("  Kiosco Centro  ", minoristaId, "loc-1", " San Martín ", "850", null,
                null, null, null, latitud, longitud);
    }

    @Test
    void elPreventorDelTokenDaDeAltaElComercioConSuZona() {
        Preventor preventor = new Preventor();
        preventor.setId("prev-1");
        Minorista minorista = new Minorista();
        minorista.setId("min-1");
        minorista.setNombre("Marta");
        minorista.setApellido("Pérez");
        Departamento capital = new Departamento();
        capital.setId("dep-1");
        capital.setNombre("Capital");
        Localidad localidad = new Localidad();
        localidad.setId("loc-1");
        localidad.setDepartamento(capital);
        when(preventorRepository.findByUsuario_IdAndEliminadoFalse("usuario-1")).thenReturn(Optional.of(preventor));
        when(minoristaRepository.findByIdAndEliminadoFalse("min-1")).thenReturn(Optional.of(minorista));
        when(localidadService.buscarEntidad("loc-1")).thenReturn(localidad);
        when(direccionRepository.save(any(Direccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(puntoRepository.save(any(PuntoDeVenta.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = service.crearPuntoDeVenta(request("min-1", -32.8878, -68.8421), "usuario-1");

        assertThat(respuesta.nombreComercio()).isEqualTo("Kiosco Centro");
        assertThat(respuesta.calle()).isEqualTo("San Martín");
        assertThat(respuesta.preventorId()).isEqualTo("prev-1");
        assertThat(respuesta.nombreMinorista()).isEqualTo("Marta Pérez");
        assertThat(respuesta.idDepartamento()).isEqualTo("dep-1");
        assertThat(respuesta.latitud()).isEqualTo(-32.8878);
    }

    @Test
    void unUsuarioSinPerfilDePreventorNoPuedeDarDeAlta() {
        when(preventorRepository.findByUsuario_IdAndEliminadoFalse("jefe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearPuntoDeVenta(request("min-1", -32.9, -68.8), "jefe"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("perfil de preventor");
        verify(puntoRepository, never()).save(any());
    }

    @Test
    void sinUbicacionEnElMapaSeRechazaSinGuardar() {
        when(preventorRepository.findByUsuario_IdAndEliminadoFalse("usuario-1"))
                .thenReturn(Optional.of(new Preventor()));
        when(minoristaRepository.findByIdAndEliminadoFalse("min-1")).thenReturn(Optional.of(new Minorista()));

        assertThatThrownBy(() -> service.crearPuntoDeVenta(request("min-1", null, -68.8), "usuario-1"))
                .isInstanceOf(ErrorServiceException.class)
                .extracting("campo").isEqualTo("latitud");
        verify(direccionRepository, never()).save(any());
        verify(puntoRepository, never()).save(any());
    }

    @Test
    void minoristaInexistenteDa404() {
        when(preventorRepository.findByUsuario_IdAndEliminadoFalse("usuario-1"))
                .thenReturn(Optional.of(new Preventor()));
        when(minoristaRepository.findByIdAndEliminadoFalse("otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearPuntoDeVenta(request("otro", -32.9, -68.8), "usuario-1"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
