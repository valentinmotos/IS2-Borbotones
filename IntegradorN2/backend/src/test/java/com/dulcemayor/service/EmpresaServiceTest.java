package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dulcemayor.dto.MayoristaRequest;
import com.dulcemayor.dto.MayoristaResponse;
import com.dulcemayor.dto.mapper.MayoristaMapperImpl;
import com.dulcemayor.entity.Mayorista;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.MayoristaRepository;
import com.dulcemayor.repository.SucursalRepository;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock private MayoristaRepository mayoristaRepository;
    @Mock private SucursalRepository sucursalRepository;
    private MayoristaService service;

    @BeforeEach
    void setUp() {
        service = new MayoristaService(mayoristaRepository, new MayoristaMapperImpl(), sucursalRepository);
    }

    @Test
    void creaLaEmpresaConCuitValidoNormalizado() {
        when(mayoristaRepository.findByEliminadoFalse()).thenReturn(List.of());
        when(mayoristaRepository.save(any(Mayorista.class))).thenAnswer(invocation -> {
            Mayorista mayorista = invocation.getArgument(0);
            mayorista.setId("empresa-1");
            return mayorista;
        });

        MayoristaResponse respuesta = service.crearMayorista(
                new MayoristaRequest(" Dulce Mayor S.A. ", "30700000008"));

        assertThat(respuesta.razonSocial()).isEqualTo("Dulce Mayor S.A.");
        assertThat(respuesta.cuit()).isEqualTo("30-70000000-8");
    }

    @Test
    void rechazaUnCuitInvalido() {
        assertThatThrownBy(() -> service.crearMayorista(new MayoristaRequest("Dulce Mayor", "20-12345678-0")))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ingresá un CUIT válido.");
        verify(mayoristaRepository, never()).save(any());
    }
}
