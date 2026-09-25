package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, String> {
    boolean existsBySubCategoria_Categoria_IdAndEliminadoFalse(String categoriaId);

    boolean existsBySubCategoria_IdAndEliminadoFalse(String subCategoriaId);
}
