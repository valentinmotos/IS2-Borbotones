package com.dulcemayor.repository;

import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, String> {
    List<Pedido> findByEliminadoFalse();
    List<Pedido> findByEstadoAndEliminadoFalse(EstadoPedido estado);
    List<Pedido> findByPreventor_IdAndEliminadoFalse(String preventorId);
    List<Pedido> findByPuntoDeVenta_IdAndEliminadoFalse(String puntoDeVentaId);
}
