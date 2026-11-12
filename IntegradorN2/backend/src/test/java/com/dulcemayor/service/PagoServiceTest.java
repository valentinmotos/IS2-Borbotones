package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.dto.PagoRequest;
import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.MonedaPago;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.FacturaRepository;
import com.dulcemayor.repository.FormaDePagoRepository;
import com.dulcemayor.repository.OrdenPagoMercadoPagoRepository;
import com.dulcemayor.repository.PagoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PagoServiceTest {
    private PagoRepository pagos;
    private FacturaRepository facturas;
    private FormaDePagoRepository formas;
    private CotizacionDolarService cotizaciones;
    private OrdenPagoMercadoPagoRepository ordenes;
    private PagoService service;
    private Factura factura;
    private FormaDePago efectivo;

    @BeforeEach
    void preparar() {
        pagos = mock(PagoRepository.class);
        facturas = mock(FacturaRepository.class);
        formas = mock(FormaDePagoRepository.class);
        cotizaciones = mock(CotizacionDolarService.class);
        ordenes = mock(OrdenPagoMercadoPagoRepository.class);
        service = new PagoService(pagos, facturas, formas, cotizaciones, ordenes);
        factura = new Factura();
        factura.setId("factura-1");
        factura.setTotal(300000);
        factura.setEstado(EstadoFactura.PENDIENTE_PAGO);
        when(facturas.buscarActivaParaActualizar("factura-1")).thenReturn(Optional.of(factura));
        efectivo = new FormaDePago();
        efectivo.setId("forma-1");
        efectivo.setTipoPago(TipoPago.EFECTIVO);
        when(formas.findById("forma-1")).thenReturn(Optional.of(efectivo));
        when(pagos.findByFactura_IdAndEliminadoFalse("factura-1")).thenReturn(List.of());
        when(ordenes.findFirstByFactura_IdAndEstadoInAndEliminadoFalseOrderByFechaCreacionDesc(
                org.mockito.ArgumentMatchers.eq("factura-1"), any())).thenReturn(Optional.empty());
        when(pagos.save(any())).thenAnswer(call -> {
            var pago = (com.dulcemayor.entity.Pago) call.getArgument(0);
            pago.setId("pago-1");
            return pago;
        });
    }

    @Test
    void registraPagoEnDolaresConLaCotizacionVigenteYConservaElEquivalenteEnPesos() {
        when(cotizaciones.buscarCotizacionVigente()).thenReturn(
                new CotizacionDolarResponse(LocalDate.of(2026, 11, 12), 1000));

        var respuesta = service.registrar(new PagoRequest("factura-1", "forma-1", 200.0, MonedaPago.USD, "Caja"));

        assertThat(respuesta.monto()).isEqualTo(200);
        assertThat(respuesta.moneda()).isEqualTo(MonedaPago.USD);
        assertThat(respuesta.cotizacionAplicada()).isEqualTo(1000);
        assertThat(respuesta.equivalenteEnPesos()).isEqualTo(200000);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PENDIENTE_PAGO);
    }

    @Test
    void rechazaUnPagoQueSupereElSaldoPendiente() {
        assertThatThrownBy(() -> service.registrar(new PagoRequest(
                "factura-1", "forma-1", 300001.0, MonedaPago.ARS, null)))
                .isInstanceOf(ErrorServiceException.class).hasMessageContaining("supera el saldo");
        verify(pagos, never()).save(any());
    }

    @Test
    void completaLaFacturaCuandoLaSumaDePagosCubreElSaldo() {
        factura.setTotal(1000);

        service.registrar(new PagoRequest("factura-1", "forma-1", 1000.0, MonedaPago.ARS, null));

        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        verify(pagos).save(any());
    }
}
