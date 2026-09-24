package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.OrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, String> {
}
