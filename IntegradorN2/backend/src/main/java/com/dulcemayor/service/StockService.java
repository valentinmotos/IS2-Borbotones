package com.dulcemayor.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.MovimientoStockResponse;
import com.dulcemayor.dto.StockResponse;
import com.dulcemayor.dto.mapper.MovimientoStockMapper;
import com.dulcemayor.entity.MovimientoStock;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Stock;
import com.dulcemayor.entity.enums.TipoMovimientoStock;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.MovimientoStockRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.StockRepository;

/**
 * E2-02: stock y movimientos de almacén.
 *
 * Cada producto tiene un único saldo ({@link Stock}) y cada cambio queda registrado como un
 * {@link MovimientoStock} con fecha, tipo y cantidad con signo. Reglas: el saldo nunca queda
 * negativo, y dos movimientos simultáneos del mismo producto se serializan con un bloqueo de
 * escritura sobre el producto (el mismo que usa E2-03). Un producto sin movimientos tiene saldo 0.
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private static final String PRODUCTO_NO_ENCONTRADO = "No se encontró el producto.";

    private final StockRepository stocks;
    private final MovimientoStockRepository movimientos;
    private final ProductoRepository productos;
    private final MovimientoStockMapper mapper;

    public StockService(StockRepository stocks, MovimientoStockRepository movimientos, ProductoRepository productos,
            MovimientoStockMapper mapper) {
        this.stocks = stocks;
        this.movimientos = movimientos;
        this.productos = productos;
        this.mapper = mapper;
    }

    /** Saldo actual de cada producto activo, ordenado por nombre. */
    public List<StockResponse> listarStock() {
        return stocks.listarStock();
    }

    /** Saldo actual de un producto activo (0 si nunca tuvo movimientos). */
    public StockResponse buscarStockPorProducto(String idProducto) {
        Producto producto = productoActivo(idProducto);
        return respuesta(producto, saldo(producto.getId()));
    }

    /** Historial de movimientos del producto, del más nuevo al más viejo. */
    public List<MovimientoStockResponse> listarMovimientos(String idProducto) {
        Producto producto = productoActivo(idProducto);
        return mapper.aRespuestas(
                movimientos.findByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(producto.getId()));
    }

    /** Para otros services (catálogo, pedidos): si alcanza el saldo para la cantidad pedida. */
    public boolean hayStock(String idProducto, int cantidad) {
        if (idProducto == null || idProducto.isBlank() || cantidad <= 0) {
            return false;
        }
        return saldo(idProducto) >= cantidad;
    }

    /** Ingreso de mercadería: suma la cantidad al saldo. */
    @Transactional
    public StockResponse reponer(String idProducto, int cantidad, String observacion) {
        validar(cantidad, "cantidad", "La cantidad del ingreso debe ser mayor que cero.");
        return registrarMovimiento(idProducto, cantidad, TipoMovimientoStock.INGRESO, observacion, "cantidad");
    }

    /** Salida por venta (E4-04): resta la cantidad. Rechaza el descuento si supera el saldo. */
    @Transactional
    public StockResponse descontar(String idProducto, int cantidad, String observacion) {
        validar(cantidad, "cantidad", "La cantidad a descontar debe ser mayor que cero.");
        return registrarMovimiento(idProducto, -cantidad, TipoMovimientoStock.EGRESO, observacion, "cantidad");
    }

    /** Firma del UML, sin observación. */
    @Transactional
    public StockResponse descontar(String idProducto, int cantidad) {
        return descontar(idProducto, cantidad, null);
    }

    /** Ajuste manual: la diferencia es positiva para sumar o negativa para descontar. */
    @Transactional
    public StockResponse ajustar(String idProducto, int diferencia, String observacion) {
        if (diferencia == 0) {
            throw new ErrorServiceException("El ajuste debe modificar el saldo.", "diferencia");
        }
        return registrarMovimiento(idProducto, diferencia, TipoMovimientoStock.AJUSTE, observacion, "diferencia");
    }

    void validar(int cantidad, String campo, String mensaje) {
        if (cantidad <= 0) {
            throw new ErrorServiceException(mensaje, campo);
        }
    }

    private StockResponse registrarMovimiento(String idProducto, int diferencia, TipoMovimientoStock tipo,
            String observacion, String campo) {
        // Bloquea la fila del producto: dos movimientos simultáneos no pueden pisar el saldo.
        Producto producto = productoParaActualizar(idProducto);
        Stock stock = stocks.findByProducto_IdAndEliminadoFalse(producto.getId()).orElseGet(() -> {
            Stock nuevo = new Stock();
            nuevo.setProducto(producto);
            nuevo.setCantidadActual(0);
            return nuevo;
        });

        int saldo;
        try {
            saldo = Math.addExact(stock.getCantidadActual(), diferencia);
        } catch (ArithmeticException ex) {
            throw new ErrorServiceException("La cantidad del movimiento supera el límite permitido.", campo);
        }
        if (saldo < 0) {
            throw new ErrorServiceException("El movimiento no puede dejar el stock en negativo (saldo actual: "
                    + stock.getCantidadActual() + ").", campo);
        }

        stock.setCantidadActual(saldo);
        stock = stocks.save(stock);

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setStock(stock);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(diferencia);
        movimiento.setFechaHora(LocalDateTime.now());
        movimiento.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.strip());
        movimientos.save(movimiento);
        return respuesta(producto, saldo);
    }

    private int saldo(String idProducto) {
        return stocks.findByProducto_IdAndEliminadoFalse(idProducto).map(Stock::getCantidadActual).orElse(0);
    }

    private StockResponse respuesta(Producto producto, int saldo) {
        return new StockResponse(producto.getId(), producto.getCodigo(), producto.getNombre(),
                producto.getPresentacion(), saldo);
    }

    private Producto productoActivo(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            throw new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO);
        }
        return productos.findByIdAndEliminadoFalse(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO));
    }

    private Producto productoParaActualizar(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            throw new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO);
        }
        return productos.buscarActivoParaActualizar(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO));
    }
}
