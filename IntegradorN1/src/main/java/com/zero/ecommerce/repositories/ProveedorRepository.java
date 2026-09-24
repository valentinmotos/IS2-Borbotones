package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, String> {
}
