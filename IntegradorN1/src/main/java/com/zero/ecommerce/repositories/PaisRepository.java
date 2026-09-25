package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Pais;

public interface PaisRepository extends JpaRepository<Pais, String> {
    List<Pais> findAllByOrderByNombreAsc();

    List<Pais> findByEliminadoFalseOrderByNombreAsc();

    Optional<Pais> findByIdAndEliminadoFalse(String id);
}
