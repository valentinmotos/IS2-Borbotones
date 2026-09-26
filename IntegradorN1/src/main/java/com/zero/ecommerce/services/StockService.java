package com.zero.ecommerce.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.MovimientoStockDTO;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.Factura;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.StockRepository;

/**
 * El stock se guarda como movimientos: cada Stock nace de un DetalleFactura y guarda en cantidadActual el saldo
 * del producto después del movimiento. El stock actual es el saldo del último movimiento. El signo lo decide la
 * factura por polimorfismo (getSignoStock): una compra a proveedor suma y una venta resta.
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    public static final String OBSERVACION_ANULACION = "Anulación";

    private final StockRepository repository;

    public StockService(StockRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra el movimiento de un detalle: nuevo saldo = saldo actual + cantidad × signo de su factura. Rechaza
     * el movimiento si el saldo quedaría negativo.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Stock registrarMovimiento(DetalleFactura detalle) throws ErrorServiceException {
        validar(detalle);
        Factura factura = detalle.getFactura();
        return crearMovimiento(detalle, detalle.getCantidad() * factura.getSignoStock(), factura.describir());
    }

    /** Movimiento inverso sobre el mismo detalle, por ejemplo al anular una venta ya pagada (E4-03). */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Stock revertirMovimiento(DetalleFactura detalle) throws ErrorServiceException {
        validar(detalle);
        return crearMovimiento(detalle, -detalle.getCantidad() * detalle.getFactura().getSignoStock(),
                OBSERVACION_ANULACION);
    }

    public void validar(DetalleFactura detalle) throws ErrorServiceException {
        if (detalle == null || detalle.getFactura() == null || detalle.getProducto() == null) {
            throw new ErrorServiceException("El movimiento de stock necesita un detalle con su factura y su producto.");
        }
        if (detalle.getCantidad() <= 0) {
            throw new ErrorServiceException("La cantidad del movimiento de stock tiene que ser mayor a 0.");
        }
    }

    private Stock crearMovimiento(DetalleFactura detalle, int cantidad, String observacion)
            throws ErrorServiceException {
        Producto producto = detalle.getProducto();
        int saldoActual = buscarStockActual(producto.getId());
        int nuevoSaldo = saldoActual + cantidad;
        if (nuevoSaldo < 0) {
            throw new ErrorServiceException("No hay stock suficiente de " + producto.getNombre() + " (talle "
                    + producto.getTalle() + "): hay " + saldoActual + " y se necesitan " + (-cantidad) + ".");
        }
        Stock stock = new Stock();
        stock.setDetalleFactura(detalle);
        stock.setProducto(producto);
        stock.setCantidadActual(nuevoSaldo);
        stock.setObservacion(observacion);
        stock.setFecha(LocalDateTime.now());
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

    /** Movimientos activos de un producto, del más reciente al más antiguo. */
    public List<Stock> listarMovimientos(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return List.of();
        }
        return repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesc(idProducto);
    }

    /**
     * Historial para la pantalla del producto. Cada movimiento guarda solo el saldo, así que la cantidad movida es
     * la diferencia con el saldo del movimiento anterior.
     */
    public List<MovimientoStockDTO> listarHistorial(String idProducto) {
        List<Stock> movimientos = listarMovimientos(idProducto);
        List<MovimientoStockDTO> historial = new ArrayList<>();
        for (int i = 0; i < movimientos.size(); i++) {
            Stock movimiento = movimientos.get(i);
            int saldoAnterior = i + 1 < movimientos.size() ? movimientos.get(i + 1).getCantidadActual() : 0;
            Factura factura = movimiento.getDetalleFactura() == null ? null : movimiento.getDetalleFactura().getFactura();
            historial.add(new MovimientoStockDTO(movimiento.getFecha(), movimiento.getObservacion(),
                    factura == null ? "-" : factura.describir(),
                    factura instanceof FacturaProveedor ? factura.getId() : null,
                    movimiento.getCantidadActual() - saldoAnterior, movimiento.getCantidadActual()));
        }
        return historial;
    }
}
