package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.Factura;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.StockRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository repository;
    @InjectMocks
    private StockService service;

    @Test
    void conMovimientosDevuelveElSaldoDelUltimo() {
        when(repository.findFirstByProducto_IdAndEliminadoFalseOrderByFechaDesc("p1"))
                .thenReturn(Optional.of(movimiento("s2", 12)));
        assertThat(service.buscarStockActual("p1")).isEqualTo(12);
    }

    @Test
    void sinMovimientosDevuelveCero() {
        when(repository.findFirstByProducto_IdAndEliminadoFalseOrderByFechaDesc("p1")).thenReturn(Optional.empty());
        assertThat(service.buscarStockActual("p1")).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void sinProductoDevuelveCeroSinConsultar(String idProducto) {
        assertThat(service.buscarStockActual(idProducto)).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    void buscaUnMovimientoActivo() throws Exception {
        Stock movimiento = movimiento("s1", 5);
        when(repository.findByIdAndEliminadoFalse("s1")).thenReturn(Optional.of(movimiento));
        assertThat(service.buscarStock("s1")).isSameAs(movimiento);
    }

    @Test
    void rechazaUnMovimientoInexistente() {
        assertThatThrownBy(() -> service.buscarStock("x"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El movimiento de stock no existe o fue eliminado.");
    }

    @Test
    void listaLosMovimientosDelMasReciente() {
        List<Stock> movimientos = List.of(movimiento("s2", 12), movimiento("s1", 5));
        when(repository.findAllByOrderByFechaDesc()).thenReturn(movimientos);
        assertThat(service.listarStock()).isEqualTo(movimientos);
    }

    @Test
    void unaCompraRecibidaSumaAlSaldoActual() throws ErrorServiceException {
        conSaldo("p1", 5);
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        FacturaProveedor compra = new FacturaProveedor();
        compra.setNumeroFactura(3);

        Stock stock = service.registrarMovimiento(detalle(compra, 20));

        assertThat(stock.getCantidadActual()).isEqualTo(25);
        assertThat(stock.getObservacion()).isEqualTo("Compra N.º 3");
        assertThat(stock.getProducto().getId()).isEqualTo("p1");
        assertThat(stock.getFecha()).isNotNull();
    }

    @Test
    void unaVentaRestaDelSaldoActual() throws ErrorServiceException {
        conSaldo("p1", 20);
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        FacturaCliente venta = new FacturaCliente();
        venta.setNumeroFactura(7);

        Stock stock = service.registrarMovimiento(detalle(venta, 17));

        assertThat(stock.getCantidadActual()).isEqualTo(3);
        assertThat(stock.getObservacion()).isEqualTo("Venta N.º 7");
    }

    @Test
    void rechazaUnMovimientoQueDejaElSaldoNegativo() {
        conSaldo("p1", 2);
        assertThatThrownBy(() -> service.registrarMovimiento(detalle(new FacturaCliente(), 3)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No hay stock suficiente de Remera (talle M): hay 2 y se necesitan 3.");
        verify(repository, never()).save(any());
    }

    @Test
    void revertirUnaVentaDevuelveLasUnidadesConLaObservacionAnulacion() throws ErrorServiceException {
        conSaldo("p1", 3);
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Stock stock = service.revertirMovimiento(detalle(new FacturaCliente(), 17));

        assertThat(stock.getCantidadActual()).isEqualTo(20);
        assertThat(stock.getObservacion()).isEqualTo("Anulación");
    }

    @Test
    void elHistorialCalculaLaCantidadMovidaConElSaldoAnterior() {
        Stock venta = movimiento("s2", 3);
        Stock compra = movimiento("s1", 20);
        when(repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesc("p1")).thenReturn(List.of(venta, compra));

        assertThat(service.listarHistorial("p1")).extracting(m -> m.cantidad()).containsExactly(-17, 20);
        assertThat(service.listarHistorial("p1")).extracting(m -> m.saldo()).containsExactly(3, 20);
    }

    private void conSaldo(String idProducto, int saldo) {
        when(repository.findFirstByProducto_IdAndEliminadoFalseOrderByFechaDesc(idProducto))
                .thenReturn(Optional.of(movimiento("anterior", saldo)));
    }

    private DetalleFactura detalle(Factura factura, int cantidad) {
        Producto producto = new Producto();
        producto.setId("p1");
        producto.setNombre("Remera");
        producto.setTalle("M");
        return factura.agregarDetalle(producto, cantidad, 1000);
    }

    private Stock movimiento(String id, int cantidadActual) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setCantidadActual(cantidadActual);
        return stock;
    }
}
