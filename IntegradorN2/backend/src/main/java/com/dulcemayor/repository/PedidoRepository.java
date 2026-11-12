package com.dulcemayor.repository;

import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PedidoRepository extends JpaRepository<Pedido, String> {
    List<Pedido> findByEliminadoFalse();
    List<Pedido> findByEstadoAndEliminadoFalse(EstadoPedido estado);
    List<Pedido> findByPreventor_IdAndEliminadoFalse(String preventorId);
    List<Pedido> findByPuntoDeVenta_IdAndEliminadoFalse(String puntoDeVentaId);

    Optional<Pedido> findByIdAndEliminadoFalse(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id and p.eliminado = false")
    Optional<Pedido> buscarActivoParaActualizar(String id);

    @EntityGraph(attributePaths = "puntoDeVenta")
    List<Pedido> findByPreventor_IdAndEliminadoFalseOrderByFechaHoraCargaDescIdDesc(String preventorId);

    boolean existsByNumeroPedido(long numeroPedido);

    /** E4-04: pedidos a procesar, en el orden en que se cargaron. */
    List<Pedido> findByEstadoInAndEliminadoFalseOrderByFechaHoraCargaAscIdAsc(Collection<EstadoPedido> estados);

    Optional<Pedido> findByPreventor_IdAndClaveCarga(String preventorId, String claveCarga);
}
