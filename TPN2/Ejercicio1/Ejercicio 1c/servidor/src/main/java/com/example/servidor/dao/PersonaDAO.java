package com.example.servidor.dao;

import com.example.servidor.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaDAO extends JpaRepository<Persona, Long> {
}
