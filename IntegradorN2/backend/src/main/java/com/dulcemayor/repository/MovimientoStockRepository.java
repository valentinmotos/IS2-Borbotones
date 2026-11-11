package com.dulcemayor.repository;

import com.dulcemayor.entity.MovimientoStock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, String> {

    java.util.List<MovimientoStock> findByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(String productoId);

    Optional<MovimientoStock> findFirstByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(
            String productoId);
}
