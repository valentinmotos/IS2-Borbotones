package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Nacionalidad;

public interface NacionalidadRepository extends JpaRepository<Nacionalidad, String> {
    List<Nacionalidad> findAllByOrderByNombreAsc();

    List<Nacionalidad> findByEliminadoFalseOrderByNombreAsc();

    Optional<Nacionalidad> findByIdAndEliminadoFalse(String id);
}
