package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AutorRepository extends JpaRepository<Autor, Long> { }