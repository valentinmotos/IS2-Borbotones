package com.example.servidor.dao;

import com.example.servidor.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonaDAO extends JpaRepository<Persona, Long> {
    List<Persona> findByFechaNacimientoIsNotNull();
}
