package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Direccion;

public interface DireccionRepository extends JpaRepository<Direccion, String> {
    Optional<Direccion> findByIdAndEliminadoFalse(String id);

    List<Direccion> findByNumeracionAndEliminadoFalse(String numeracion);
}
