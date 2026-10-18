package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
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

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FormaDePagoRepository;

@ExtendWith(MockitoExtension.class)
class FormaDePagoServiceTest {

    @Mock
    private FormaDePagoRepository repository;
    @InjectMocks
    private FormaDePagoService service;

    @Test
    void rechazaDuplicadoActivoConMismoTipoYObservacion() {
        when(repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc())
                .thenReturn(List.of(formaDePago("1", TipoPago.BILLETERA_VIRTUAL, "Mercado Pago")));
        assertThatThrownBy(() -> service.crearFormaDePago(TipoPago.BILLETERA_VIRTUAL, "  MERCADO PAGO  "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Ya existe una forma de pago activa con ese tipo y observación.");
        verify(repository, never()).save(any());
    }

    @Test
    void permiteMismaObservacionConOtroTipo() throws Exception {
        when(repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc())
                .thenReturn(List.of(formaDePago("1", TipoPago.TRANSFERENCIA, "Banco Nación")));
        service.crearFormaDePago(TipoPago.EFECTIVO, "Banco Nación");
        verify(repository).save(any());
    }

    @Test
    void exigeTipoDePago() {
        assertThatThrownBy(() -> service.crearFormaDePago(null, "Efectivo"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El tipo de pago es obligatorio.");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t\n" })
    void exigeObservacion(String observacion) {
        assertThatThrownBy(() -> service.crearFormaDePago(TipoPago.EFECTIVO, observacion))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La observación de la forma de pago es obligatoria.");
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaObservacionDemasiadoLarga() {
        assertThatThrownBy(() -> service.crearFormaDePago(TipoPago.EFECTIVO, "x".repeat(256)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La observación no puede superar los 255 caracteres.");
        verify(repository, never()).save(any());
    }

    @Test
    void creaConObservacionSinEspaciosEnLosExtremos() throws Exception {
        service.crearFormaDePago(TipoPago.BILLETERA_VIRTUAL, " Mercado Pago ");
        ArgumentCaptor<FormaDePago> guardada = ArgumentCaptor.forClass(FormaDePago.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getTipoPago()).isEqualTo(TipoPago.BILLETERA_VIRTUAL);
        assertThat(guardada.getValue().getObservacion()).isEqualTo("Mercado Pago");
        assertThat(guardada.getValue().isEliminado()).isFalse();
    }

    @Test
    void permiteEditarSinCambiarLosDatos() throws Exception {
        FormaDePago actual = formaDePago("1", TipoPago.EFECTIVO, "Efectivo");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        when(repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc()).thenReturn(List.of(actual));
        service.modificarFormaDePago("1", TipoPago.EFECTIVO, " Efectivo ");
        verify(repository).save(actual);
    }

    @Test
    void rechazaEditarConDatosDeOtraSinModificarLaEntidad() {
        FormaDePago actual = formaDePago("1", TipoPago.EFECTIVO, "Efectivo");
        FormaDePago otra = formaDePago("2", TipoPago.TRANSFERENCIA, "Transferencia");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        when(repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc()).thenReturn(List.of(actual, otra));
        assertThatThrownBy(() -> service.modificarFormaDePago("1", TipoPago.TRANSFERENCIA, "Transferencia"))
                .isInstanceOf(ErrorServiceException.class);
        assertThat(actual.getTipoPago()).isEqualTo(TipoPago.EFECTIVO);
        assertThat(actual.getObservacion()).isEqualTo("Efectivo");
        verify(repository, never()).save(any());
    }

    @Test
    void eliminaLogicamente() throws Exception {
        FormaDePago actual = formaDePago("1", TipoPago.EFECTIVO, "Efectivo");
        when(repository.findByIdAndEliminadoFalse("1")).thenReturn(Optional.of(actual));
        service.eliminarFormaDePago("1");
        assertThat(actual.isEliminado()).isTrue();
        verify(repository).save(actual);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void rechazaModificarIdInexistente() {
        assertThatThrownBy(() -> service.modificarFormaDePago("inexistente", TipoPago.EFECTIVO, "Efectivo"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La forma de pago no existe o fue eliminada.");
        verify(repository, never()).save(any());
    }

    @Test
    void convierteElTipoDePagoRecibido() throws Exception {
        assertThat(service.convertirTipoPago("BILLETERA_VIRTUAL")).isEqualTo(TipoPago.BILLETERA_VIRTUAL);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void convertirExigeTipoDePago(String tipoPago) {
        assertThatThrownBy(() -> service.convertirTipoPago(tipoPago))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El tipo de pago es obligatorio.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "CHEQUE", "efectivo" })
    void convertirRechazaTipoInexistente(String tipoPago) {
        assertThatThrownBy(() -> service.convertirTipoPago(tipoPago))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El tipo de pago no es válido.");
    }

    @Test
    void listaLosTiposDePagoEnElOrdenDelEnum() {
        assertThat(service.listarTipoPago()).containsExactly(
                entry("EFECTIVO", "Efectivo"),
                entry("TRANSFERENCIA", "Transferencia"),
                entry("BILLETERA_VIRTUAL", "Billetera virtual"));
    }

    @Test
    void armaLasFilasDelListadoConLasActivas() {
        when(repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc())
                .thenReturn(List.of(formaDePago("1", TipoPago.BILLETERA_VIRTUAL, "Mercado Pago")));
        assertThat(service.listarFilaFormaDePagoActivo()).containsExactly(
                new FilaTablaDTO("1", "Mercado Pago (Billetera virtual)", List.of("Billetera virtual", "Mercado Pago")));
    }

    private FormaDePago formaDePago(String id, TipoPago tipoPago, String observacion) {
        FormaDePago formaDePago = new FormaDePago();
        formaDePago.setId(id);
        formaDePago.setTipoPago(tipoPago);
        formaDePago.setObservacion(observacion);
        return formaDePago;
    }
}
