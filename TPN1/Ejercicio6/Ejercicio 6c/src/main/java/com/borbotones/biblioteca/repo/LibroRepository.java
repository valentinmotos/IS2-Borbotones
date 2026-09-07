package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LibroRepository extends JpaRepository<Libro, Long> { }