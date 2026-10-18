package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Localidad;

public interface LocalidadRepository extends JpaRepository<Localidad, String> {
    List<Localidad> findAllByOrderByNombreAsc();

    List<Localidad> findByEliminadoFalseOrderByNombreAsc();

    List<Localidad> findByDepartamento_IdOrderByNombreAsc(String departamentoId);

    List<Localidad> findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(String departamentoId);

    List<Localidad> findByCodigoPostalAndEliminadoFalseOrderByNombreAsc(String codigoPostal);

    Optional<Localidad> findByIdAndEliminadoFalse(String id);

    boolean existsByDepartamento_IdAndEliminadoFalse(String departamentoId);
}
