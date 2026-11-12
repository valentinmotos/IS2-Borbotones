package com.dulcemayor.repository;

import com.dulcemayor.entity.HistorialEstadoPedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, String> {
    List<HistorialEstadoPedido> findByPedido_IdOrderByFechaHoraAsc(String pedidoId);

    @EntityGraph(attributePaths = {"usuario", "transportista"})
    List<HistorialEstadoPedido> findByPedido_IdAndEliminadoFalseOrderByFechaHoraAscIdAsc(String pedidoId);

    Optional<HistorialEstadoPedido> findFirstByPedido_IdAndEliminadoFalseOrderByFechaHoraDescIdDesc(String pedidoId);
}
