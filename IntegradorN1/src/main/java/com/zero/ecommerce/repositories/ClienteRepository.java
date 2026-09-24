package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
}
