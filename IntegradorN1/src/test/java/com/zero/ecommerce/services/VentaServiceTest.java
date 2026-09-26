package com.zero.ecommerce.services;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private FacturaClienteRepository facturaClienteRepository;
    @Mock
    private OrdenCompraService ordenCompraService;

    @InjectMocks
    private VentaService service;

    private OrdenCompra orden;
    private FacturaCliente factura;

    @BeforeEach
    void setUp() {
        orden = new OrdenCompra();
        orden.setId("o1");
        factura = new FacturaCliente();
        factura.setEstado(EstadoFactura.SIN_DEFINIR);
    }

    @Test
    void elClienteAnulaUnaCompraSinPagarYSeAnulaTambienLaFactura() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_PAGO);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);
        when(ordenCompraService.buscarFacturaDePedido("o1")).thenReturn(Optional.of(factura));

        service.anularVenta("o1", false);

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.ANULADA);
        verify(ordenCompraRepository).save(orden);
        verify(facturaClienteRepository).save(factura);
    }

    @Test
    void elClienteNoPuedeAnularUnaCompraPagada() throws Exception {
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_ENVIO);
        when(ordenCompraService.buscarPedido("o1")).thenReturn(orden);

        assertThatThrownBy(() -> service.anularVenta("o1", false))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue pagada. Para anularla comunicate con la tienda.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
        verify(ordenCompraRepository, never()).save(any());
    }
}
