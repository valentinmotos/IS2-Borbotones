package com.dulcemayor.repository;

import com.dulcemayor.entity.HistorialEstadoPedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, String> {
    List<HistorialEstadoPedido> findByPedido_IdOrderByFechaHoraAsc(String pedidoId);

    @EntityGraph(attributePaths = {"usuario", "transportista"})
    List<HistorialEstadoPedido> findByPedido_IdAndEliminadoFalseOrderByFechaHoraAscIdAsc(String pedidoId);

    Optional<HistorialEstadoPedido> findFirstByPedido_IdAndEliminadoFalseOrderByFechaHoraDescIdDesc(String pedidoId);

    /** E5-02: el último registro de un transportista en un estado (la última entrega del distribuidor). */
    Optional<HistorialEstadoPedido> findFirstByTransportista_IdAndEstadoAndEliminadoFalseOrderByFechaHoraDescIdDesc(
            String transportistaId, EstadoPedido estado);
}
