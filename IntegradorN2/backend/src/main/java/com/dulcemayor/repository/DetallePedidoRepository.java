package com.dulcemayor.repository;

import com.dulcemayor.entity.DetallePedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, String> {
    List<DetallePedido> findByPedido_IdAndEliminadoFalse(String pedidoId);
}
