package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Pais;

public interface PaisRepository extends JpaRepository<Pais, String> {
}
