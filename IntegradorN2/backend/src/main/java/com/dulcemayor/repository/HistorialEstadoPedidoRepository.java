package com.dulcemayor.repository;

import com.dulcemayor.entity.HistorialEstadoPedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, String> {
    List<HistorialEstadoPedido> findByPedido_IdOrderByFechaHoraAsc(String pedidoId);
}
