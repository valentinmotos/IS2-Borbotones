package com.zero.ecommerce.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Stock movimiento(String id, int cantidadActual) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setCantidadActual(cantidadActual);
        return stock;
    }
}
