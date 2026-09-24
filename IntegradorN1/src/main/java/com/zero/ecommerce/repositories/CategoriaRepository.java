package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {
}
