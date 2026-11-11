package com.dulcemayor.repository;

import com.dulcemayor.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findByProducto_Id(String productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Stock> findByProducto_IdAndEliminadoFalse(String productoId);

    java.util.List<Stock> findByEliminadoFalseOrderByProducto_NombreAsc();
}
