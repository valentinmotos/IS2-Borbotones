package com.example.servidor.dao;

import com.example.servidor.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface LibroDAO extends JpaRepository<Libro, Long> {
    @Query("select l from Libro l where not exists (select p.id from Prestamo p where p.libro = l and (p.devuelto = false or p.devuelto is null)) order by l.titulo, l.id")
    List<Libro> findLibrosDisponibles();
}
