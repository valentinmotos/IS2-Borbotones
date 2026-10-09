package com.example.servidor.dao;

import com.example.servidor.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.servidor.model.Persona;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoDAO extends JpaRepository<Prestamo, Long> {
    @Query("select distinct p.persona from Prestamo p where p.persona is not null order by p.persona.apellido, p.persona.nombre, p.persona.id")
    List<Persona> findPersonasConAlquileres();
    List<Prestamo> findByFechaDevolucionAndDevueltoFalse(LocalDate fechaDevolucion);
}
