package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
public interface LibroRepository extends JpaRepository<Libro, Long>, RevisionRepository<Libro, Long, Integer> { }
