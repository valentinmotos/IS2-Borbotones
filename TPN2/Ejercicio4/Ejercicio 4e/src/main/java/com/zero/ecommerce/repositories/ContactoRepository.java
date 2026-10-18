package com.zero.ecommerce.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Contacto;

public interface ContactoRepository extends JpaRepository<Contacto, String> {
    Optional<Contacto> findByIdAndEliminadoFalse(String id);
}
