package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
public interface AutorRepository extends JpaRepository<Autor, Long>, RevisionRepository<Autor, Long, Integer> { }
