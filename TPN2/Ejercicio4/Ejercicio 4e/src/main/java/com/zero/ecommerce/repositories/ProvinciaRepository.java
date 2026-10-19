package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Provincia;

public interface ProvinciaRepository extends JpaRepository<Provincia, String> {
    List<Provincia> findAllByOrderByNombreAsc();

    List<Provincia> findByEliminadoFalseOrderByNombreAsc();

    List<Provincia> findByPais_IdOrderByNombreAsc(String paisId);

    List<Provincia> findByPais_IdAndEliminadoFalseOrderByNombreAsc(String paisId);

    Optional<Provincia> findByIdAndEliminadoFalse(String id);

    boolean existsByPais_IdAndEliminadoFalse(String paisId);
}
