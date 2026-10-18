package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.enums.TipoEmpresa;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.EmpresaRepository;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    private static final String CUIT = "30-71234567-1";

    @Mock
    private EmpresaRepository repository;
    @Mock
    private DireccionService direccionService;
    @Mock
    private ContactoService contactoService;
    @InjectMocks
    private EmpresaService service;

    @Test
    void rechazaUnCuitConDigitoVerificadorIncorrecto() {
        assertThatThrownBy(() -> crear("30-71234567-2", TipoEmpresa.SUCURSAL))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageStartingWith("El CUIT no es válido");
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnCuitRepetidoAunqueVengaSinGuiones() {
        when(repository.findByCuitAndEliminadoFalse(CUIT)).thenReturn(Optional.of(empresa("otra", TipoEmpresa.SUCURSAL)));
        assertThatThrownBy(() -> crear("30712345671", TipoEmpresa.SUCURSAL))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una empresa con ese CUIT.");
    }

    @Test
    void soloPuedeHaberUnaSedeCentral() {
        when(repository.findFirstByTipoSucursalAndEliminadoFalse(TipoEmpresa.SEDE_CENTRAL))
                .thenReturn(Optional.of(empresa("sede", TipoEmpresa.SEDE_CENTRAL)));
        assertThatThrownBy(() -> crear(CUIT, TipoEmpresa.SEDE_CENTRAL))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una sede central. Solo puede haber una.");
        verify(repository, never()).save(any());
    }

    @Test
    void laSedeCentralNoPuedePasarASucursal() {
        Empresa sede = empresa("sede", TipoEmpresa.SEDE_CENTRAL);
        when(repository.findByIdAndEliminadoFalse("sede")).thenReturn(Optional.of(sede));
        when(repository.findFirstByTipoSucursalAndEliminadoFalse(TipoEmpresa.SEDE_CENTRAL)).thenReturn(Optional.of(sede));
        assertThatThrownBy(() -> service.modificarEmpresa("sede", "Zero", CUIT, TipoEmpresa.SUCURSAL,
                new DireccionForm(), "contacto@zero.com.ar", "261 4231250", TipoTelefono.FIJO))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La sede central no puede pasar a ser sucursal.");
    }

    @Test
    void noSeEliminaLaSedeCentral() {
        when(repository.findByIdAndEliminadoFalse("sede")).thenReturn(Optional.of(empresa("sede", TipoEmpresa.SEDE_CENTRAL)));
        assertThatThrownBy(() -> service.eliminarEmpresa("sede"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede eliminar la sede central.");
        verify(repository, never()).save(any());
    }

    @Test
    void exigeLaRazonSocial() {
        assertThatThrownBy(() -> service.crearEmpresa(" ", CUIT, TipoEmpresa.SUCURSAL, new DireccionForm(),
                "contacto@zero.com.ar", "261 4231250", TipoTelefono.FIJO))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La razón social es obligatoria.");
    }

    private void crear(String cuit, TipoEmpresa tipo) throws ErrorServiceException {
        service.crearEmpresa("Zero Sucursal", cuit, tipo, new DireccionForm(), "contacto@zero.com.ar",
                "261 4231250", TipoTelefono.FIJO);
    }

    private Empresa empresa(String id, TipoEmpresa tipo) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setTipoSucursal(tipo);
        return empresa;
    }
}
