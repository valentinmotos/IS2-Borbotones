package com.zero.ecommerce.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.ProductoStockDTO;
import com.zero.ecommerce.dto.ReporteStockDTO;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Stock;
import com.zero.ecommerce.entities.enums.EstadoStock;
import com.zero.ecommerce.exception.ErrorServiceException;

/** Construye el reporte de productos y stock de RF29. */
@Service
@Transactional(readOnly = true)
public class ReporteStockService {

    private final StockService stockService;
    private final EmpresaService empresaService;

    public ReporteStockService(StockService stockService, EmpresaService empresaService) {
        this.stockService = stockService;
        this.empresaService = empresaService;
    }

    /**
     * El stock de referencia es el saldo que dejó la última recepción. Como los movimientos vienen del más
     * reciente al más antiguo, el primer movimiento de cada producto da el saldo actual y la primera compra
     * encontrada da la referencia. Los productos sin una compra recibida no forman parte del reporte.
     */
    public ReporteStockDTO generar() throws ErrorServiceException {
        Empresa sede = empresaService.buscarSedeCentral();
        Map<String, MovimientosProducto> porProducto = new LinkedHashMap<>();

        for (Stock movimiento : stockService.listarStockActivo()) {
            Producto producto = movimiento.getProducto();
            if (producto == null || producto.isEliminado()) {
                continue;
            }
            MovimientosProducto movimientos = porProducto.computeIfAbsent(producto.getId(),
                    id -> new MovimientosProducto(producto, movimiento.getCantidadActual()));
            if (movimientos.stockReferencia == null && esRecepcion(movimiento)) {
                movimientos.stockReferencia = movimiento.getCantidadActual();
            }
        }

        List<ProductoStockDTO> productos = new ArrayList<>();
        for (MovimientosProducto movimientos : porProducto.values()) {
            if (movimientos.stockReferencia == null || movimientos.stockReferencia <= 0) {
                continue;
            }
            productos.add(aDTO(movimientos));
        }
        productos.sort((a, b) -> {
            int nombre = a.nombre().compareToIgnoreCase(b.nombre());
            return nombre != 0 ? nombre : a.talle().compareToIgnoreCase(b.talle());
        });

        int totalUnidades = productos.stream().mapToInt(ProductoStockDTO::stockActual).sum();
        int buenos = contar(productos, EstadoStock.BUENO);
        int regulares = contar(productos, EstadoStock.REGULAR);
        int malos = contar(productos, EstadoStock.MALO);
        return new ReporteStockDTO(sede.getId(), sede.getRazonSocial(), totalUnidades,
                buenos, regulares, malos, List.copyOf(productos));
    }

    private boolean esRecepcion(Stock movimiento) {
        return movimiento.getDetalleFactura() != null
                && movimiento.getDetalleFactura().getFactura() instanceof FacturaProveedor;
    }

    private ProductoStockDTO aDTO(MovimientosProducto movimientos) {
        Producto producto = movimientos.producto;
        double porcentajeExacto = movimientos.stockActual * 100.0 / movimientos.stockReferencia;
        int porcentaje = (int) Math.round(porcentajeExacto);
        int porcentajeBarra = Math.max(0, Math.min(100, porcentaje));
        EstadoStock estado = EstadoStock.desdePorcentaje(porcentajeExacto);
        return new ProductoStockDTO(producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getTalle(),
                producto.getSubCategoria().getCategoria().getId(),
                producto.getSubCategoria().getCategoria().getNombre(), producto.getSubCategoria().getNombre(),
                movimientos.stockActual, movimientos.stockReferencia, porcentaje, porcentajeBarra, estado);
    }

    private int contar(List<ProductoStockDTO> productos, EstadoStock estado) {
        return (int) productos.stream().filter(producto -> producto.estado() == estado).count();
    }

    private static final class MovimientosProducto {
        private final Producto producto;
        private final int stockActual;
        private Integer stockReferencia;

        private MovimientosProducto(Producto producto, int stockActual) {
            this.producto = producto;
            this.stockActual = stockActual;
        }
    }
}
