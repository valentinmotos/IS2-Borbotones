package com.borbotones.videojuegos.repositories;

import com.borbotones.videojuegos.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long>, RevisionRepository<Categoria, Long, Integer> {
}
