package com.dulcemayor.repository;

import com.dulcemayor.entity.Categoria;

import java.util.List;

public interface CategoriaRepository extends BaseRepository<Categoria> {
    List<Categoria> findByEliminadoFalse();
}
