package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.ConfiguracionCorreoEmpresa;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ConfiguracionCorreoEmpresaRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionCorreoEmpresaServiceTest {

    @Mock
    private ConfiguracionCorreoEmpresaRepository repository;
    @Mock
    private EmpresaService empresaService;
    @InjectMocks
    private ConfiguracionCorreoEmpresaService service;

    @Test
    void modificarConLaClaveVaciaConservaLaGuardada() throws Exception {
        Empresa sede = new Empresa();
        sede.setId("sede");
        ConfiguracionCorreoEmpresa configuracion = new ConfiguracionCorreoEmpresa();
        configuracion.setId("1");
        configuracion.setClave("clave-guardada");
        configuracion.setEmpresa(sede);
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(configuracion));
        when(empresaService.buscarEmpresa("sede")).thenReturn(sede);
        when(repository.findByEmpresa_IdAndEliminadoFalse("sede")).thenReturn(Optional.of(configuracion));

        service.modificarConfiguracionCorreoEmpresa("1", "tienda@gmail.com", "  ", "587", "smtp.gmail.com", true, "sede");

        assertThat(configuracion.getClave()).isEqualTo("clave-guardada");
        assertThat(configuracion.getCorreo()).isEqualTo("tienda@gmail.com");
        verify(repository).save(configuracion);
    }

    @Test
    void laClaveEsObligatoriaEnElAlta() {
        assertThatThrownBy(() -> service.crearConfiguracionCorreoEmpresa("tienda@gmail.com", "", "587",
                "smtp.gmail.com", true, "sede"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La clave del correo es obligatoria.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "65536", "abc", "58 7" })
    void rechazaPuertosInvalidos(String puerto) {
        assertThatThrownBy(() -> service.crearConfiguracionCorreoEmpresa("tienda@gmail.com", "clave", puerto,
                "smtp.gmail.com", true, "sede"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El puerto tiene que ser un número entre 1 y 65535.");
    }

    @Test
    void rechazaUnCorreoSinFormato() {
        assertThatThrownBy(() -> service.crearConfiguracionCorreoEmpresa("tienda-gmail.com", "clave", "587",
                "smtp.gmail.com", true, "sede"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El correo no tiene un formato válido.");
    }
}
