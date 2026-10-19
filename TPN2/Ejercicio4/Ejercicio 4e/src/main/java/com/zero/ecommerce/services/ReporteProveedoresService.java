package com.zero.ecommerce.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.PrecioProveedorDTO;
import com.zero.ecommerce.dto.ProductoProveedorDTO;
import com.zero.ecommerce.dto.ReporteProveedoresDTO;
import com.zero.ecommerce.entities.DetalleFactura;
import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.enums.EstadoFactura;
import com.zero.ecommerce.utils.ExportadorCsv;
import com.zero.ecommerce.utils.TextoUtils;

/**
 * Reporte de proveedores (RF31): por producto, qué le cobró cada proveedor la última vez y cuál es el más económico.
 * Se arma con los DetalleFactura de las compras recibidas (FacturaProveedor en PAGADA): las pedidas y las anuladas no
 * cuentan, porque la mercadería no llegó. Se comparan los últimos precios de cada proveedor, así un precio viejo que
 * ya no se consigue no define la recomendación.
 */
@Service
@Transactional(readOnly = true)
public class ReporteProveedoresService {

    // La compra más reciente primero: por fecha y, en el mismo día, por número.
    private static final Comparator<UltimaCompra> MAS_RECIENTE = Comparator
            .comparing((UltimaCompra u) -> u.compra().getFechaFactura(), Comparator.reverseOrder())
            .thenComparing(u -> u.compra().getNumeroFactura(), Comparator.reverseOrder());

    // El más barato primero. Con el mismo precio, el de la compra más reciente.
    private static final Comparator<UltimaCompra> MAS_ECONOMICO = Comparator
            .comparingDouble(UltimaCompra::precio)
            .thenComparing(MAS_RECIENTE);

    private final FacturaProveedorService facturaProveedorService;

    public ReporteProveedoresService(FacturaProveedorService facturaProveedorService) {
        this.facturaProveedorService = facturaProveedorService;
    }

    /** El reporte completo: todos los productos activos que tienen al menos una compra recibida. */
    public ReporteProveedoresDTO generar() {
        return generar(null, null);
    }

    /**
     * El reporte filtrado. texto busca por código o nombre del producto, sin importar mayúsculas ni tildes, e
     * idCategoria filtra por la categoría de su subcategoría. Cada filtro es opcional (null o vacío no filtra).
     */
    public ReporteProveedoresDTO generar(String texto, String idCategoria) {
        String buscado = vacio(texto) ? null : TextoUtils.normalizar(texto);
        List<ProductoProveedorDTO> filas = listarUltimasComprasPorProducto().values().stream()
                .filter(ultimas -> coincide(ultimas.get(0).detalle().getProducto(), buscado, idCategoria))
                .map(this::armarFila)
                .sorted(Comparator.comparing(ProductoProveedorDTO::nombre).thenComparing(ProductoProveedorDTO::talle))
                .toList();
        int proveedores = (int) filas.stream()
                .flatMap(fila -> fila.proveedores().stream())
                .map(PrecioProveedorDTO::idProveedor)
                .distinct()
                .count();
        int conVariosProveedores = (int) filas.stream().filter(fila -> fila.proveedores().size() > 1).count();
        return new ReporteProveedoresDTO(filas.size(), proveedores, conVariosProveedores, filas);
    }

    /** El reporte de proveedores filtrado, en CSV (E5-02). */
    public byte[] exportarCsv(String texto, String idCategoria) {
        ReporteProveedoresDTO reporte = generar(texto, idCategoria);
        List<String> encabezados = List.of("Código", "Producto", "Talle", "Categoría", "Subcategoría",
                "Proveedor recomendado", "Precio de costo más bajo", "Fecha de compra");
        List<List<String>> filas = new ArrayList<>();
        for (ProductoProveedorDTO p : reporte.productos()) {
            PrecioProveedorDTO recomendado = p.recomendado();
            filas.add(Arrays.asList(p.codigo(), p.nombre(), p.talle(), p.categoria(), p.subCategoria(),
                    recomendado == null ? "" : recomendado.razonSocial(),
                    recomendado == null ? "" : ExportadorCsv.monto(recomendado.precioCosto()),
                    recomendado == null ? "" : ExportadorCsv.fecha(recomendado.fechaCompra())));
        }
        return ExportadorCsv.exportar(encabezados, filas);
    }

    /**
     * El proveedor con el último precio de costo más bajo para el producto (lo usa la reposición por WhatsApp de
     * E5-04). Vacío si el producto no tiene compras recibidas de ningún proveedor activo: ahí se elige a mano.
     */
    public Optional<Proveedor> buscarProveedorMasEconomico(String idProducto) {
        if (vacio(idProducto)) {
            return Optional.empty();
        }
        return Optional.ofNullable(listarUltimasComprasPorProducto().get(idProducto))
                .map(ultimas -> ultimas.get(0).compra().getProveedor());
    }

    /**
     * Producto → la última compra recibida de cada proveedor activo que lo vendió, de la más económica a la más cara.
     * Se recorren las compras de la más reciente a la más vieja y se guarda solo la primera de cada proveedor.
     */
    private Map<String, List<UltimaCompra>> listarUltimasComprasPorProducto() {
        List<UltimaCompra> renglones = new ArrayList<>();
        for (FacturaProveedor compra : facturaProveedorService.listarFacturaPorEstado(EstadoFactura.PAGADA)) {
            if (compra.getProveedor().isEliminado()) {
                continue;
            }
            for (DetalleFactura detalle : compra.getDetalles()) {
                if (!detalle.isEliminado() && !detalle.getProducto().isEliminado()) {
                    renglones.add(new UltimaCompra(compra, detalle));
                }
            }
        }
        renglones.sort(MAS_RECIENTE);

        Map<String, Map<String, UltimaCompra>> porProducto = new LinkedHashMap<>();
        for (UltimaCompra renglon : renglones) {
            porProducto.computeIfAbsent(renglon.detalle().getProducto().getId(), id -> new LinkedHashMap<>())
                    .putIfAbsent(renglon.compra().getProveedor().getId(), renglon);
        }
        Map<String, List<UltimaCompra>> resultado = new LinkedHashMap<>();
        porProducto.forEach((idProducto, porProveedor) ->
                resultado.put(idProducto, porProveedor.values().stream().sorted(MAS_ECONOMICO).toList()));
        return resultado;
    }

    // ultimas viene ordenada de la más económica a la más cara: la primera es la recomendada.
    private ProductoProveedorDTO armarFila(List<UltimaCompra> ultimas) {
        double masBajo = ultimas.get(0).precio();
        List<PrecioProveedorDTO> precios = new ArrayList<>();
        for (UltimaCompra ultima : ultimas) {
            FacturaProveedor compra = ultima.compra();
            precios.add(new PrecioProveedorDTO(compra.getProveedor().getId(), compra.getProveedor().getRazonSocial(),
                    ultima.precio(), compra.getFechaFactura(), compra.getId(), compra.getNumeroFactura(),
                    (ultima.precio() - masBajo) / masBajo * 100, precios.isEmpty()));
        }
        Producto producto = ultimas.get(0).detalle().getProducto();
        SubCategoria subCategoria = producto.getSubCategoria();
        return new ProductoProveedorDTO(producto.getId(), producto.getCodigo(), producto.getNombre(),
                producto.getTalle(), subCategoria.getCategoria().getNombre(), subCategoria.getNombre(), precios.get(0),
                precios);
    }

    private boolean coincide(Producto producto, String buscado, String idCategoria) {
        boolean coincideTexto = buscado == null || contiene(producto.getCodigo(), buscado)
                || contiene(producto.getNombre(), buscado);
        return coincideTexto
                && (vacio(idCategoria) || producto.getSubCategoria().getCategoria().getId().equals(idCategoria));
    }

    private boolean contiene(String valor, String buscado) {
        return valor != null && TextoUtils.normalizar(valor).contains(buscado);
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Un renglón de una compra recibida, junto con su compra (que tiene el proveedor y la fecha). */
    private record UltimaCompra(FacturaProveedor compra, DetalleFactura detalle) {

        double precio() {
            return detalle.getPrecioUnitario();
        }
    }
}
