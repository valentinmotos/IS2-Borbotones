package com.dulcemayor.repository;

import com.dulcemayor.entity.MovimientoStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, String> {

    /** E2-02: historial del producto, del más nuevo al más viejo. */
    List<MovimientoStock> findByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(String productoId);

    Optional<MovimientoStock> findFirstByStock_Producto_IdAndEliminadoFalseOrderByFechaHoraDesc(
            String productoId);
}
