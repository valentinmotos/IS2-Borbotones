package com.dulcemayor.repository;

import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.enums.EstadoFactura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FacturaRepository extends JpaRepository<Factura, String> {
    Optional<Factura> findByPedido_IdAndEliminadoFalse(String pedidoId);
    List<Factura> findByEliminadoFalse();
    List<Factura> findByEstadoAndEliminadoFalse(EstadoFactura estado);
    List<Factura> findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(String minoristaId);

    Optional<Factura> findByIdAndEliminadoFalse(String id);

    /**
     * E4-04: último número emitido (0 si no hay facturas). Cuenta también las dadas de baja para que
     * un número nunca se reutilice.
     */
    @Query("select coalesce(max(f.numeroFactura), 0) from Factura f")
    long ultimoNumeroFactura();

    /** E4-04: listados del panel, de la más nueva a la más vieja. */
    @EntityGraph(attributePaths = {"pedido", "pedido.puntoDeVenta"})
    List<Factura> findByEliminadoFalseOrderByNumeroFacturaDesc();

    @EntityGraph(attributePaths = {"pedido", "pedido.puntoDeVenta"})
    List<Factura> findByEstadoAndEliminadoFalseOrderByNumeroFacturaDesc(EstadoFactura estado);
}
