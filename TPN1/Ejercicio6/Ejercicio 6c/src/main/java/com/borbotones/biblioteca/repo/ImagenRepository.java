package com.borbotones.biblioteca.repo;

import com.borbotones.biblioteca.model.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

public interface ImagenRepository extends JpaRepository<Imagen, Long>, RevisionRepository<Imagen, Long, Integer> { }
