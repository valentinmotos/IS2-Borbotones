package com.example.servidor.dao;

import com.example.servidor.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoDAO extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByFechaDevolucionAndDevueltoFalse(LocalDate fechaDevolucion);
}
