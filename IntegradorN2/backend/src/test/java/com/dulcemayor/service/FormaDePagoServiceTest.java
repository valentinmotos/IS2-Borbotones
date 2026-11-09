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
import com.dulcemayor.dto.FormaDePagoRequest;
import com.dulcemayor.dto.mapper.FormaDePagoMapperImpl;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.FormaDePagoRepository;

@ExtendWith(MockitoExtension.class)
class FormaDePagoServiceTest {

    @Mock private FormaDePagoRepository formaRepository;
    private FormaDePagoService service;

    @BeforeEach
    void setUp() { service = new FormaDePagoService(formaRepository, new FormaDePagoMapperImpl()); }

    @Test
    void transferenciaRequiereAlias() {
        assertThatThrownBy(() -> service.crearFormaDePago(new FormaDePagoRequest(TipoPago.TRANSFERENCIA, "  ")))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El alias es obligatorio para la transferencia.");
        verify(formaRepository, never()).save(any());
    }

    @Test
    void creaFormaDePagoActiva() {
        when(formaRepository.findByEliminadoFalse()).thenReturn(List.of());
        when(formaRepository.save(any(FormaDePago.class))).thenAnswer(invocation -> {
            FormaDePago forma = invocation.getArgument(0);
            forma.setId("pago-1");
            return forma;
        });

        var respuesta = service.crearFormaDePago(new FormaDePagoRequest(TipoPago.EFECTIVO, null));

        assertThat(respuesta.tipoPago()).isEqualTo(TipoPago.EFECTIVO);
        assertThat(respuesta.activo()).isTrue();
    }
}
