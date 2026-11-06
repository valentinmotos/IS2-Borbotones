package com.dulcemayor.repository;

import com.dulcemayor.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findByProducto_Id(String productoId);
}
