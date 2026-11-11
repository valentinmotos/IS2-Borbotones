package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dulcemayor.dto.mapper.MovimientoStockMapperImpl;
import com.dulcemayor.entity.MovimientoStock;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Stock;
import com.dulcemayor.entity.enums.TipoMovimientoStock;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.MovimientoStockRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.StockRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    private static final String ID_PRODUCTO = "producto-1";

    @Mock private StockRepository stockRepository;
    @Mock private MovimientoStockRepository movimientoRepository;
    @Mock private ProductoRepository productoRepository;
    private StockService service;
    private Producto producto;

    @BeforeEach
    void setUp() {
        service = new StockService(stockRepository, movimientoRepository, productoRepository,
                new MovimientoStockMapperImpl());
        producto = new Producto();
        producto.setId(ID_PRODUCTO);
        producto.setCodigo("ALF-001");
        producto.setNombre("Alfajor");
        producto.setPresentacion("Caja x24");
    }

    /** Simula las tablas en memoria: el saldo guardado queda disponible para el próximo movimiento. */
    private List<MovimientoStock> tablasEnMemoria() {
        List<Stock> saldos = new ArrayList<>();
        List<MovimientoStock> historial = new ArrayList<>();
        when(productoRepository.buscarActivoParaActualizar(ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(stockRepository.findByProducto_IdAndEliminadoFalse(ID_PRODUCTO))
                .thenAnswer(inv -> saldos.stream().findFirst());
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> {
            Stock stock = inv.getArgument(0);
            if (stock.getId() == null) {
                stock.setId("stock-1");
                saldos.add(stock);
            }
            return stock;
        });
        when(movimientoRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> {
            historial.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        return historial;
    }

    @Test
    void unaReposicionDe20LlevaElSaldoDe0A20() {
        List<MovimientoStock> historial = tablasEnMemoria();

        var respuesta = service.reponer(ID_PRODUCTO, 20, "  Remito 123  ");

        assertThat(respuesta.cantidadActual()).isEqualTo(20);
        assertThat(respuesta.sinStock()).isFalse();
        assertThat(historial).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimientoStock.INGRESO);
            assertThat(m.getCantidad()).isEqualTo(20);
            assertThat(m.getObservacion()).isEqualTo("Remito 123");
            assertThat(m.getFechaHora()).isNotNull();
        });
    }

    @Test
    void unDescuentoMayorAlSaldoSeRechazaYNoRegistraMovimiento() {
        List<MovimientoStock> historial = tablasEnMemoria();
        service.reponer(ID_PRODUCTO, 20, null);

        assertThatThrownBy(() -> service.descontar(ID_PRODUCTO, 21))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("negativo")
                .extracting("campo").isEqualTo("cantidad");
        assertThat(historial).hasSize(1);

        var respuesta = service.descontar(ID_PRODUCTO, 20);
        assertThat(respuesta.cantidadActual()).isZero();
        assertThat(respuesta.sinStock()).isTrue();
        assertThat(historial.get(1).getTipo()).isEqualTo(TipoMovimientoStock.EGRESO);
        assertThat(historial.get(1).getCantidad()).isEqualTo(-20);
    }

    @Test
    void unAjusteQueDejaElSaldoNegativoSeRechazaConElCampoDiferencia() {
        tablasEnMemoria();
        service.reponer(ID_PRODUCTO, 5, null);

        assertThatThrownBy(() -> service.ajustar(ID_PRODUCTO, -6, "Rotura"))
                .isInstanceOf(ErrorServiceException.class)
                .extracting("campo").isEqualTo("diferencia");
        assertThat(service.ajustar(ID_PRODUCTO, -2, "Rotura").cantidadActual()).isEqualTo(3);
    }

    @Test
    void rechazaCantidadesInvalidasSinTocarLaBase() {
        assertThatThrownBy(() -> service.reponer(ID_PRODUCTO, 0, null))
                .isInstanceOf(ErrorServiceException.class).extracting("campo").isEqualTo("cantidad");
        assertThatThrownBy(() -> service.descontar(ID_PRODUCTO, -1))
                .isInstanceOf(ErrorServiceException.class).extracting("campo").isEqualTo("cantidad");
        assertThatThrownBy(() -> service.ajustar(ID_PRODUCTO, 0, null))
                .isInstanceOf(ErrorServiceException.class).extracting("campo").isEqualTo("diferencia");
        verify(stockRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void productoInexistenteDa404() {
        when(productoRepository.buscarActivoParaActualizar("otro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reponer("otro", 10, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void hayStockComparaContraElSaldoYEsFalsoSinMovimientos() {
        when(stockRepository.findByProducto_IdAndEliminadoFalse("sin-movimientos")).thenReturn(Optional.empty());
        Stock stock = new Stock();
        stock.setCantidadActual(10);
        when(stockRepository.findByProducto_IdAndEliminadoFalse(ID_PRODUCTO)).thenReturn(Optional.of(stock));

        assertThat(service.hayStock(ID_PRODUCTO, 10)).isTrue();
        assertThat(service.hayStock(ID_PRODUCTO, 11)).isFalse();
        assertThat(service.hayStock("sin-movimientos", 1)).isFalse();
        assertThat(service.hayStock(ID_PRODUCTO, 0)).isFalse();
    }
}
