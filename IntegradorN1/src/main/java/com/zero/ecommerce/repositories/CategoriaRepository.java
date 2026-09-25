package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {
    List<Categoria> findAllByOrderByNombreAsc();

    List<Categoria> findByEliminadoFalseOrderByNombreAsc();

    Optional<Categoria> findByIdAndEliminadoFalse(String id);

    Optional<Categoria> findByNombreIgnoreCaseAndEliminadoFalse(String nombre);
}
