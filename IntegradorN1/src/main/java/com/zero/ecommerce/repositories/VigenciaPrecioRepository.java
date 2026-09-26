package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.VigenciaPrecio;

public interface VigenciaPrecioRepository extends JpaRepository<VigenciaPrecio, String> {

    List<VigenciaPrecio> findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(String productoId);

    List<VigenciaPrecio> findByEliminadoFalseOrderByFechaDesdeAsc();

    Optional<VigenciaPrecio> findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(String productoId);
}
