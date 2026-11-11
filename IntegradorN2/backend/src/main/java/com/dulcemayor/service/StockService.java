package com.dulcemayor.service;

import com.dulcemayor.dto.AjusteStockRequest;
import com.dulcemayor.dto.MovimientoStockRequest;
import com.dulcemayor.dto.MovimientoStockResponse;
import com.dulcemayor.dto.ProductoResponse;
import com.dulcemayor.dto.StockResponse;
import com.dulcemayor.entity.MovimientoStock;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Stock;
import com.dulcemayor.entity.enums.TipoMovimientoStock;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.MovimientoStockRepository;
import com.dulcemayor.repository.StockRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {
    private final StockRepository stocks;
    private final MovimientoStockRepository movimientos;
    private final ProductoService productos;

    public StockService(StockRepository stocks, MovimientoStockRepository movimientos, ProductoService productos) {
        this.stocks = stocks;
        this.movimientos = movimientos;
        this.productos = productos;
    }

    public List<StockResponse> listar() {
        return productos.listarActivos().stream().map(producto -> {
            int cantidad = stocks.findByProducto_Id(producto.id()).filter(s -> !s.isEliminado())
                    .map(Stock::getCantidadActual).orElse(0);
            return respuesta(producto, cantidad);
        }).toList();
    }

    public List<MovimientoStockResponse> listarMovimientos(String productoId) {
        productos.buscarEntidad(productoId);
        return movimientos.findByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(productoId)
                .stream().map(m -> new MovimientoStockResponse(m.getId(), m.getTipo(), m.getCantidad(),
                        m.getFechaHora(), m.getObservacion())).toList();
    }

    @Transactional
    public StockResponse ingresar(MovimientoStockRequest request) {
        if (request.cantidad() == null || request.cantidad() <= 0) {
            throw new ErrorServiceException("La cantidad del ingreso debe ser mayor que cero.", "cantidad");
        }
        return mover(request.productoId(), request.cantidad(), TipoMovimientoStock.INGRESO, request.observacion());
    }

    @Transactional
    public StockResponse ajustar(AjusteStockRequest request) {
        if (request.diferencia() == null || request.diferencia() == 0) {
            throw new ErrorServiceException("El ajuste debe modificar el saldo.", "diferencia");
        }
        return mover(request.productoId(), request.diferencia(), TipoMovimientoStock.AJUSTE, request.observacion());
    }

    private StockResponse mover(String productoId, int diferencia, TipoMovimientoStock tipo, String observacion) {
        Producto producto = productos.buscarParaMovimientoStock(productoId);
        Stock stock = stocks.findByProducto_IdAndEliminadoFalse(productoId).orElseGet(() -> {
            Stock nuevo = new Stock();
            nuevo.setProducto(producto);
            nuevo.setCantidadActual(0);
            return nuevo;
        });
        final int saldo;
        try {
            saldo = Math.addExact(stock.getCantidadActual(), diferencia);
        } catch (ArithmeticException ex) {
            throw new ErrorServiceException("La cantidad del movimiento supera el límite permitido.", "cantidad");
        }
        if (saldo < 0) {
            throw new ErrorServiceException("El movimiento no puede dejar el stock en negativo.", "cantidad");
        }
        stock.setCantidadActual(saldo);
        stock = stocks.saveAndFlush(stock);
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setStock(stock);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(diferencia);
        movimiento.setFechaHora(LocalDateTime.now());
        movimiento.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.strip());
        movimientos.save(movimiento);
        return respuesta(new ProductoResponse(producto.getId(), producto.getCodigo(), producto.getNombre(),
                producto.getDescripcion(), producto.getPresentacion(), producto.getUnidadesPorBulto(),
                producto.getCategoria().getId(), producto.getCategoria().getNombre(),
                producto.getImagen() == null ? null : producto.getImagen().getId()), saldo);
    }

    private StockResponse respuesta(ProductoResponse producto, int cantidad) {
        return new StockResponse(producto.id(), producto.codigo(), producto.nombre(), producto.presentacion(),
                cantidad, cantidad == 0);
    }
}
