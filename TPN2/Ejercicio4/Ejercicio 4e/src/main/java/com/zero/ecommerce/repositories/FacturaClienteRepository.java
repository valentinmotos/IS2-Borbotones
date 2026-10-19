package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.enums.EstadoFactura;

public interface FacturaClienteRepository extends JpaRepository<FacturaCliente, String> {

    /** La factura de un pedido: la relación está solo del lado de la factura (FacturaCliente → OrdenCompra). */
    Optional<FacturaCliente> findFirstByOrdenCompra_IdAndEliminadoFalse(String ordenCompraId);

    /** Las facturas asociadas a un pedido, para armar el listado de pedidos sin una consulta por fila. */
    List<FacturaCliente> findByOrdenCompraIsNotNullAndEliminadoFalse();

    /** Todas las ventas pagadas ordenadas por fecha, para reportes y dashboard. */
    List<FacturaCliente> findByEliminadoFalseOrderByFechaFacturaAsc();

    /** La venta con el número más alto (incluidas las eliminadas), para la numeración secuencial. */
    Optional<FacturaCliente> findFirstByOrderByNumeroFacturaDesc();

    /** Reporte de ventas (E5-01): facturas de clientes por estado (PAGADA) y rango de fechas de factura. */
    @EntityGraph(attributePaths = { "formaDePago", "ordenCompra", "detalles", "detalles.producto",
            "detalles.producto.subCategoria", "detalles.producto.subCategoria.categoria" })
    List<FacturaCliente> findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
            EstadoFactura estado, java.time.LocalDate desde, java.time.LocalDate hasta);
}
