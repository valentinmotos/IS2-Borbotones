package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Stock;

public interface StockRepository extends JpaRepository<Stock, String> {
}
