package com.zero.ecommerce.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.StockRepository;

/**
 * El stock se guarda como movimientos: cada Stock guarda en cantidadActual el saldo del producto
 * después del movimiento. Acá está la parte de lectura; la escritura (crearStock, registrar y
 * revertir movimientos) se implementa en E3-03.
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository repository;
    private final ProductoRepository productoRepository;

    public StockService(StockRepository repository, ProductoRepository productoRepository) {
        this.repository = repository;
        this.productoRepository = productoRepository;
    }

    /**
     * Carga inicial usada exclusivamente por los datos de demostracion. No reemplaza los
     * movimientos de compras y no duplica el saldo si el producto ya tiene movimientos.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Stock cargarStockInicial(String idProducto, int cantidad) throws ErrorServiceException {
        if (cantidad <= 0) {
            throw new ErrorServiceException("El stock inicial debe ser mayor a 0.");
        }
        var producto = productoRepository.findByIdAndEliminadoFalse(idProducto)
                .orElseThrow(() -> new ErrorServiceException("El producto no existe o fue eliminado."));
        var existente = repository.findFirstByProducto_IdAndEliminadoFalseOrderByFechaDesc(idProducto);
        if (existente.isPresent()) {
            return existente.get();
        }
        Stock stock = new Stock();
        stock.setProducto(producto);
        stock.setCantidadActual(cantidad);
        stock.setFecha(LocalDateTime.now());
        stock.setObservacion("Stock inicial de demostracion");
        return repository.save(stock);
    }

    /**
     * Stock actual de un producto: el saldo de su último movimiento, o 0 si nunca tuvo movimientos.
     * El diagrama devuelve el Stock, pero el contrato acordado en el kickoff de la Etapa 2 es un int.
     */
    public int buscarStockActual(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return 0;
        }
        return repository.findFirstByProducto_IdAndEliminadoFalseOrderByFechaDesc(idProducto)
                .map(Stock::getCantidadActual)
                .orElse(0);
    }

    public Stock buscarStock(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El movimiento de stock no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El movimiento de stock no existe o fue eliminado."));
    }

    /** Todos los movimientos, del más reciente al más antiguo. */
    public List<Stock> listarStock() {
        return repository.findAllByOrderByFechaDesc();
    }
}
