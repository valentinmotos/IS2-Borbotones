package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dulcemayor.dto.PerfilMinoristaRequest;
import com.dulcemayor.dto.mapper.PerfilMinoristaMapperImpl;
import com.dulcemayor.entity.Departamento;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Localidad;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DireccionRepository;
import com.dulcemayor.repository.MinoristaRepository;

@ExtendWith(MockitoExtension.class)
class MinoristaServiceTest {

    private static final String ID_USUARIO = "usuario-1";

    @Mock private MinoristaRepository minoristaRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private LocalidadService localidadService;
    private MinoristaService service;
    private Minorista minorista;

    @BeforeEach
    void setUp() {
        service = new MinoristaService(minoristaRepository,
                new DireccionService(direccionRepository, localidadService), new PerfilMinoristaMapperImpl());
        Usuario usuario = new Usuario();
        usuario.setId(ID_USUARIO);
        usuario.setNombreUsuario("minorista@prueba.com");
        minorista = new Minorista();
        minorista.setId("min-1");
        minorista.setNombre("Marta");
        minorista.setApellido("Pérez");
        minorista.setFechaNacimiento(LocalDate.of(1985, 1, 1));
        minorista.setUsuario(usuario);
    }

    private static Localidad localidad(String id, String departamentoId) {
        Departamento departamento = new Departamento();
        departamento.setId(departamentoId);
        departamento.setNombre("Departamento " + departamentoId);
        Localidad localidad = new Localidad();
        localidad.setId(id);
        localidad.setNombre("Localidad " + id);
        localidad.setDepartamento(departamento);
        return localidad;
    }

    private static PerfilMinoristaRequest request(String localidadId, LocalDate nacimiento, Double latitud) {
        return new PerfilMinoristaRequest(" Marta ", " Gómez ", nacimiento, localidadId, "San Martín", "850",
                null, null, null, null, latitud, -68.84);
    }

    private void perfilGuardable() {
        when(minoristaRepository.findByUsuario_IdAndEliminadoFalse(ID_USUARIO)).thenReturn(Optional.of(minorista));
        when(direccionRepository.save(any(Direccion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(minoristaRepository.save(any(Minorista.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void editaLosDatosYLaDireccionQuedaEnLaLocalidadElegida() {
        perfilGuardable();
        when(localidadService.buscarEntidad("godoy-cruz")).thenReturn(localidad("godoy-cruz", "dep-gc"));

        var perfil = service.modificarMinorista(ID_USUARIO, request("godoy-cruz", LocalDate.of(1985, 1, 1), -32.92));

        assertThat(perfil.nombre()).isEqualTo("Marta");
        assertThat(perfil.apellido()).isEqualTo("Gómez");
        assertThat(perfil.correo()).isEqualTo("minorista@prueba.com");
        assertThat(perfil.localidadId()).isEqualTo("godoy-cruz");
        assertThat(perfil.departamentoId()).isEqualTo("dep-gc");
        assertThat(perfil.latitud()).isEqualTo(-32.92);
        assertThat(minorista.getDireccionEntrega()).isNotNull();
    }

    @Test
    void unSegundoGuardadoModificaLaMismaDireccion() {
        perfilGuardable();
        when(localidadService.buscarEntidad("capital")).thenReturn(localidad("capital", "dep-c"));
        when(localidadService.buscarEntidad("godoy-cruz")).thenReturn(localidad("godoy-cruz", "dep-gc"));

        service.modificarMinorista(ID_USUARIO, request("capital", LocalDate.of(1985, 1, 1), -32.89));
        Direccion primera = minorista.getDireccionEntrega();
        var perfil = service.modificarMinorista(ID_USUARIO, request("godoy-cruz", LocalDate.of(1985, 1, 1), -32.92));

        assertThat(minorista.getDireccionEntrega()).isSameAs(primera);
        assertThat(perfil.localidadId()).isEqualTo("godoy-cruz");
        verify(direccionRepository, times(2)).save(primera);
    }

    @Test
    void rechazaUnMenorDeEdadSinGuardar() {
        assertThatThrownBy(() -> service.modificarMinorista(ID_USUARIO,
                request("capital", LocalDate.now().minusYears(17), -32.89)))
                .isInstanceOf(ErrorServiceException.class)
                .extracting("campo").isEqualTo("fechaNacimiento");
        verify(minoristaRepository, never()).save(any());
    }

    @Test
    void rechazaLaDireccionSinUbicacion() {
        when(minoristaRepository.findByUsuario_IdAndEliminadoFalse(ID_USUARIO)).thenReturn(Optional.of(minorista));

        assertThatThrownBy(() -> service.modificarMinorista(ID_USUARIO,
                request("capital", LocalDate.of(1985, 1, 1), null)))
                .isInstanceOf(ErrorServiceException.class)
                .extracting("campo").isEqualTo("latitud");
        verify(direccionRepository, never()).save(any());
        verify(minoristaRepository, never()).save(any());
    }

    @Test
    void unUsuarioSinPerfilDeMinoristaDa404() {
        when(minoristaRepository.findByUsuario_IdAndEliminadoFalse("otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPerfil("otro")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
