package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FacturaCliente;

public interface FacturaClienteRepository extends JpaRepository<FacturaCliente, String> {
}
