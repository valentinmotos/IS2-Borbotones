package com.dulcemayor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dulcemayor.entity.Categoria;
import com.dulcemayor.entity.MovimientoStock;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Stock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MovimientoStockRepositoryTest {

    @Autowired private MovimientoStockRepository movimientoStockRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private StockRepository stockRepository;

    @Test
    void devuelveElMovimientoMasRecienteDelProducto() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Alfajores");
        categoria = categoriaRepository.save(categoria);

        Producto producto = new Producto();
        producto.setCodigo("ALF-001");
        producto.setNombre("Alfajor");
        producto.setPresentacion("Caja x12");
        producto.setUnidadesPorBulto(12);
        producto.setCategoria(categoria);
        producto = productoRepository.save(producto);

        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(10);
        stock = stockRepository.save(stock);

        MovimientoStock anterior = movimiento(stock, 12, LocalDateTime.of(2026, 1, 10, 9, 0));
        MovimientoStock reciente = movimiento(stock, 2, LocalDateTime.of(2026, 2, 15, 14, 30));
        movimientoStockRepository.save(anterior);
        reciente = movimientoStockRepository.save(reciente);
        MovimientoStock eliminado = movimiento(stock, 99, LocalDateTime.of(2026, 3, 1, 8, 0));
        eliminado.setEliminado(true);
        movimientoStockRepository.save(eliminado);

        Producto otroProducto = new Producto();
        otroProducto.setCodigo("ALF-002");
        otroProducto.setNombre("Otro alfajor");
        otroProducto.setPresentacion("Caja x6");
        otroProducto.setUnidadesPorBulto(6);
        otroProducto.setCategoria(categoria);
        productoRepository.save(otroProducto);
        Stock otroStock = new Stock();
        otroStock.setProducto(otroProducto);
        otroStock.setCantidadActual(20);
        stockRepository.save(otroStock);
        movimientoStockRepository.save(movimiento(otroStock, 20, LocalDateTime.of(2026, 4, 1, 8, 0)));

        assertThat(movimientoStockRepository
                .findFirstByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(producto.getId()))
                .contains(reciente);
        assertThat(movimientoStockRepository
                .findFirstByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc("inexistente"))
                .isEmpty();
    }

    private MovimientoStock movimiento(Stock stock, int cantidad, LocalDateTime fechaHora) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setStock(stock);
        movimiento.setCantidad(cantidad);
        movimiento.setFechaHora(fechaHora);
        return movimiento;
    }
}
