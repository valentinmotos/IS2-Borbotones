package com.dulcemayor.repository;

import com.dulcemayor.entity.VigenciaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VigenciaPrecioRepository extends JpaRepository<VigenciaPrecio, String> {
    Optional<VigenciaPrecio> findFirstByProducto_IdAndFechaHastaIsNullAndEliminadoFalseOrderByFechaDesdeDesc(
            String productoId);
    List<VigenciaPrecio> findByProducto_IdAndEliminadoFalseOrderByFechaDesdeDesc(String productoId);
}
