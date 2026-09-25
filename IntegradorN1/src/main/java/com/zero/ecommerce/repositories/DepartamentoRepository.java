package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Departamento;

public interface DepartamentoRepository extends JpaRepository<Departamento, String> {
    List<Departamento> findAllByOrderByNombreAsc();

    List<Departamento> findByEliminadoFalseOrderByNombreAsc();

    List<Departamento> findByProvincia_IdOrderByNombreAsc(String provinciaId);

    List<Departamento> findByProvincia_IdAndEliminadoFalseOrderByNombreAsc(String provinciaId);

    Optional<Departamento> findByIdAndEliminadoFalse(String id);

    boolean existsByProvincia_IdAndEliminadoFalse(String provinciaId);
}
