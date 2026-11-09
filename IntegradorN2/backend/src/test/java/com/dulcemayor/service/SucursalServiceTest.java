package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dulcemayor.dto.SucursalRequest;
import com.dulcemayor.dto.mapper.SucursalMapperImpl;
import com.dulcemayor.entity.Mayorista;
import com.dulcemayor.entity.Sucursal;
import com.dulcemayor.entity.enums.TipoEmpresa;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.repository.DireccionRepository;
import com.dulcemayor.repository.LocalidadRepository;
import com.dulcemayor.repository.MayoristaRepository;
import com.dulcemayor.repository.SucursalRepository;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    @Mock private SucursalRepository sucursalRepository;
    @Mock private MayoristaRepository mayoristaRepository;
    @Mock private LocalidadRepository localidadRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private ImagenService imagenService;
    private SucursalService service;

    @BeforeEach
    void setUp() {
        service = new SucursalService(sucursalRepository, new SucursalMapperImpl(), mayoristaRepository,
                localidadRepository, direccionRepository, imagenService);
    }

    @Test
    void rechazaUnaSegundaSedeCentralActiva() {
        Mayorista empresa = new Mayorista();
        empresa.setId("empresa-1");
        Sucursal central = new Sucursal();
        central.setId("sucursal-1");
        central.setMayorista(empresa);
        central.setTipo(TipoEmpresa.SEDE_CENTRAL);
        when(mayoristaRepository.findByIdAndEliminadoFalse("empresa-1")).thenReturn(Optional.of(empresa));
        when(sucursalRepository.findByEliminadoFalse()).thenReturn(List.of(central));

        SucursalRequest request = new SucursalRequest("Casa Nueva", TipoEmpresa.SEDE_CENTRAL,
                "empresa-1", "localidad-1", "San Martín", "123", null, null, null,
                null, -32.89, -68.84, "imagen-1");

        assertThatThrownBy(() -> service.crearSucursal(request))
                .isInstanceOf(DuplicadoException.class)
                .hasMessage("La empresa ya tiene una sede central activa.");
        verify(sucursalRepository, never()).save(any());
    }
}
