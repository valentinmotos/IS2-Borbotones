package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Direccion;

public interface DireccionRepository extends JpaRepository<Direccion, String> {
}
