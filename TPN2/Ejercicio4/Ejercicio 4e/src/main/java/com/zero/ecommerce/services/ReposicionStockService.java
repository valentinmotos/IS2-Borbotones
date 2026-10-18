package com.zero.ecommerce.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.zero.ecommerce.dto.ProductoStockDTO;
import com.zero.ecommerce.dto.ReposicionStockDTO;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.EstadoStock;
import com.zero.ecommerce.exception.ErrorServiceException;

/** Calcula y prepara las acciones de reposición del reporte de stock (RF30). */
@Service
@Transactional(readOnly = true)
public class ReposicionStockService {

    private final ReporteStockService reporteStockService;
    private final ReporteProveedoresService reporteProveedoresService;
    private final ProveedorService proveedorService;

    public ReposicionStockService(ReporteStockService reporteStockService,
            ReporteProveedoresService reporteProveedoresService, ProveedorService proveedorService) {
        this.reporteStockService = reporteStockService;
        this.reporteProveedoresService = reporteProveedoresService;
        this.proveedorService = proveedorService;
    }

    /** Reposiciones por id de producto, solamente para las filas con estado Malo. */
    public Map<String, ReposicionStockDTO> listar(List<ProductoStockDTO> productos) {
        Map<String, ReposicionStockDTO> reposiciones = new LinkedHashMap<>();
        productos.stream()
                .filter(producto -> producto.estado() == EstadoStock.MALO)
                .map(this::armar)
                .forEach(reposicion -> reposiciones.put(reposicion.productoId(), reposicion));
        return reposiciones;
    }

    /**
     * URL de WhatsApp para el proveedor recomendado o para el elegido a mano. Se vuelve a calcular el reporte para
     * no confiar en cantidades recibidas desde el navegador.
     */
    public String generarUrlWhatsApp(String idProducto, String idProveedor) throws ErrorServiceException {
        ProductoStockDTO producto = reporteStockService.generar().productos().stream()
                .filter(fila -> fila.productoId().equals(idProducto))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("El producto no forma parte del reporte de stock."));
        if (producto.estado() != EstadoStock.MALO) {
            throw new ErrorServiceException("Solo se puede pedir reposición para productos con stock Malo.");
        }

        Proveedor proveedor = idProveedor == null || idProveedor.isBlank()
                ? reporteProveedoresService.buscarProveedorMasEconomico(idProducto)
                        .orElseThrow(() -> new ErrorServiceException("Elegí un proveedor para pedir la reposición."))
                : proveedorService.buscarProveedor(idProveedor);
        ContactoTelefonico celular = proveedor.buscarCelularActivo()
                .orElseThrow(() -> new ErrorServiceException(
                        "El proveedor seleccionado no tiene un celular activo para WhatsApp."));
        String mensaje = armarMensaje(proveedor, producto, calcularUnidades(producto));
        return UriComponentsBuilder.fromUriString("https://wa.me/" + celular.getTelefono())
                .queryParam("text", mensaje)
                .encode()
                .build()
                .toUriString();
    }

    private ReposicionStockDTO armar(ProductoStockDTO producto) {
        Proveedor proveedor = reporteProveedoresService.buscarProveedorMasEconomico(producto.productoId())
                .orElse(null);
        return new ReposicionStockDTO(producto.productoId(), producto.codigo(), producto.nombre(), producto.talle(),
                calcularUnidades(producto), proveedor == null ? null : proveedor.getId(),
                proveedor == null ? null : proveedor.getRazonSocial());
    }

    /** El objetivo es la mitad de la referencia, redondeada hacia arriba. */
    int calcularUnidades(ProductoStockDTO producto) {
        int objetivo = (int) Math.ceil(producto.stockReferencia() * 0.5);
        return objetivo - producto.stockActual();
    }

    private String armarMensaje(Proveedor proveedor, ProductoStockDTO producto, int unidades) {
        return "Hola, " + proveedor.getRazonSocial() + ".\n"
                + "Quisiéramos solicitar la reposición del siguiente producto:\n"
                + "- Producto: " + producto.nombre() + "\n"
                + "- Código: " + producto.codigo() + "\n"
                + "- Talle: " + producto.talle() + "\n"
                + "- Cantidad: " + unidades + " unidades\n\n"
                + "Saludos,\nZero";
    }
}
