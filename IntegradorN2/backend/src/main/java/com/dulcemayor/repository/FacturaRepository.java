package com.dulcemayor.repository;

import com.dulcemayor.entity.Factura;
import com.dulcemayor.entity.enums.EstadoFactura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, String> {
    Optional<Factura> findByPedido_IdAndEliminadoFalse(String pedidoId);
    List<Factura> findByEliminadoFalse();
    List<Factura> findByEstadoAndEliminadoFalse(EstadoFactura estado);
    List<Factura> findByPedido_PuntoDeVenta_Minorista_IdAndEliminadoFalse(String minoristaId);
}
