package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.SubCategoria;

public interface SubCategoriaRepository extends JpaRepository<SubCategoria, String> {
    List<SubCategoria> findAllByOrderByNombreAsc();

    List<SubCategoria> findByEliminadoFalseOrderByNombreAsc();

    List<SubCategoria> findByCategoria_IdAndEliminadoFalseOrderByNombreAsc(String categoriaId);

    Optional<SubCategoria> findByIdAndEliminadoFalse(String id);

    Optional<SubCategoria> findByCategoria_IdAndNombreIgnoreCaseAndEliminadoFalse(String categoriaId, String nombre);
}
