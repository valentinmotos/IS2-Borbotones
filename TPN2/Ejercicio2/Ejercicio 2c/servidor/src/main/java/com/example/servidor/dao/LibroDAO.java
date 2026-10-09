package com.example.servidor.dao;

import com.example.servidor.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroDAO extends JpaRepository<Libro, Long> {
}
