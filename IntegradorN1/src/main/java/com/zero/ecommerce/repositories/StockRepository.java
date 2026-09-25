package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Stock;

public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findAllByOrderByFechaDesc();

    Optional<Stock> findByIdAndEliminadoFalse(String id);

    /** Último movimiento de un producto: su cantidadActual es el stock actual. */
    Optional<Stock> findFirstByProducto_IdAndEliminadoFalseOrderByFechaDesc(String idProducto);
}
