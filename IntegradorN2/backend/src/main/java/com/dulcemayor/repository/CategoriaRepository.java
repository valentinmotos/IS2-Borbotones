package com.dulcemayor.repository;

import com.dulcemayor.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {
    List<Categoria> findByEliminadoFalse();
}
