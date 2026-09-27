package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;

@ExtendWith(MockitoExtension.class)
class MercadoPagoServiceTest {

    @Mock
    private OrdenCompraService ordenCompraService;

    @Mock
    private VentaService ventaService;

    private MercadoPagoService mercadoPagoService;

    @BeforeEach
    void setUp() {
        mercadoPagoService = new MercadoPagoService(
                "TEST-ACCESS-TOKEN",
                "http://localhost:8080",
                ordenCompraService,
                ventaService);
    }

    @Test
    void crearPreferenciaOrdenNulaLanzaExcepcion() {
        assertThatThrownBy(() -> mercadoPagoService.crearPreferencia(null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("nula");
    }

    @Test
    void crearPreferenciaSinItemsLanzaExcepcion() {
        OrdenCompra orden = new OrdenCompra();
        orden.setId("o1");

        assertThatThrownBy(() -> mercadoPagoService.crearPreferencia(orden))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("sin ítems");
    }

    @Test
    void procesarNotificacionPaymentIdNuloNoHaceNada() throws Exception {
        mercadoPagoService.procesarNotificacion(null);
        verify(ordenCompraService, never()).buscarPorIdentificadorCompra(null);
    }
}
