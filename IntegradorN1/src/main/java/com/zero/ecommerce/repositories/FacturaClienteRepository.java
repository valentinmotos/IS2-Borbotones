package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FacturaCliente;

public interface FacturaClienteRepository extends JpaRepository<FacturaCliente, String> {

    /** La factura de un pedido: la relación está solo del lado de la factura (FacturaCliente → OrdenCompra). */
    Optional<FacturaCliente> findFirstByOrdenCompra_IdAndEliminadoFalse(String ordenCompraId);

    /** Las facturas asociadas a un pedido, para armar el listado de pedidos sin una consulta por fila. */
    List<FacturaCliente> findByOrdenCompraIsNotNullAndEliminadoFalse();

    /** La venta con el número más alto (incluidas las eliminadas), para la numeración secuencial. */
    Optional<FacturaCliente> findFirstByOrderByNumeroFacturaDesc();

    /** Reporte de ventas (E5-01): facturas de clientes por estado (PAGADA) y rango de fechas de factura. */
    List<FacturaCliente> findByEstadoAndFechaFacturaBetweenAndEliminadoFalseOrderByFechaFacturaDescNumeroFacturaDesc(
            com.zero.ecommerce.entities.enums.EstadoFactura estado, java.time.LocalDate desde, java.time.LocalDate hasta);
}
