package com.zero.ecommerce.services;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.zero.ecommerce.utils.ExportadorCsv;

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

    /** Los productos del reporte con el estado y la categoría elegidos. Un filtro en null o vacío no filtra. */
    public List<ProductoStockDTO> filtrar(ReporteStockDTO reporte, EstadoStock estado, String idCategoria) {
        return reporte.productos().stream()
                .filter(producto -> estado == null || producto.estado() == estado)
                .filter(producto -> idCategoria == null || idCategoria.isBlank()
                        || producto.categoriaId().equals(idCategoria))
                .toList();
    }

    /** El estado que llega del filtro de la pantalla, o null si viene vacío o no existe. */
    public EstadoStock convertirEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoStock.valueOf(estado.strip().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** El reporte de stock con los mismos filtros de la pantalla, en CSV (E5-02). */
    public byte[] exportarCsv(String estado, String idCategoria) throws ErrorServiceException {
        List<String> encabezados = List.of("Código", "Producto", "Talle", "Categoría", "Subcategoría",
                "Stock actual", "Stock de referencia", "Porcentaje", "Estado");
        List<List<String>> filas = new ArrayList<>();
        for (ProductoStockDTO p : filtrar(generar(), convertirEstado(estado), idCategoria)) {
            filas.add(Arrays.asList(p.codigo(), p.nombre(), p.talle(), p.categoriaNombre(), p.subCategoriaNombre(),
                    String.valueOf(p.stockActual()), String.valueOf(p.stockReferencia()), p.porcentaje() + " %",
                    p.estado().getDescripcion()));
        }
        return ExportadorCsv.exportar(encabezados, filas);
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
