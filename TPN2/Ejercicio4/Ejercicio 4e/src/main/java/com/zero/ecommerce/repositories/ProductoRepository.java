package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, String> {
    boolean existsByIdAndEliminadoFalse(String id);

    boolean existsBySubCategoria_Categoria_IdAndEliminadoFalse(String categoriaId);

    boolean existsBySubCategoria_IdAndEliminadoFalse(String subCategoriaId);

    List<Producto> findAllByOrderByNombreAscTalleAsc();

    List<Producto> findByEliminadoFalseOrderByNombreAscTalleAsc();

    Optional<Producto> findByIdAndEliminadoFalse(String id);
}
