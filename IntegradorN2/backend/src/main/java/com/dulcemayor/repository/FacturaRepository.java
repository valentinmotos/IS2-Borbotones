package com.dulcemayor.repository;

import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, String> {
    Optional<Factura> findByPedido_IdAndEliminadoFalse(String pedidoId);
    List<Factura> findByEliminadoFalse();
    List<Factura> findByEstadoAndEliminadoFalse(EstadoFactura estado);
    List<Factura> findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(String minoristaId);

    Optional<Factura> findByIdAndEliminadoFalse(String id);

    /** E4-08: una factura solo si es de un punto de venta del minorista indicado. */
    Optional<Factura> findByIdAndPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(String id, String minoristaId);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Factura f where f.id = :id and f.eliminado = false")
    Optional<Factura> buscarActivaParaActualizar(@Param("id") String id);

    /** E5-03: ventas facturadas no anuladas, con los datos necesarios para el reporte. */
    @EntityGraph(attributePaths = {"pedido", "pedido.puntoDeVenta", "pedido.puntoDeVenta.minorista",
            "pedido.puntoDeVenta.direccion.localidad.departamento", "pedido.preventor"})
    @Query("""
            select f from Factura f
            join f.pedido p
            join p.puntoDeVenta pv
            join pv.direccion d
            join d.localidad l
            where f.eliminado = false
              and p.eliminado = false
              and f.estado <> :facturaAnulada
              and p.estado in :estadosVenta
              and f.fechaFactura between :desde and :hasta
              and (:departamentoId is null or l.departamento.id = :departamentoId)
              and (:preventorId is null or p.preventor.id = :preventorId)
            order by f.fechaFactura desc, f.numeroFactura desc
            """)
    List<Factura> listarVentasParaReporte(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("departamentoId") String departamentoId, @Param("preventorId") String preventorId,
            @Param("facturaAnulada") EstadoFactura facturaAnulada,
            @Param("estadosVenta") List<EstadoPedido> estadosVenta);
}
