package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Persona;

public interface PersonaRepository extends JpaRepository<Persona, String> {
}
