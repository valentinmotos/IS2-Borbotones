package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Imagen;

public interface ImagenRepository extends JpaRepository<Imagen, String> {
}
