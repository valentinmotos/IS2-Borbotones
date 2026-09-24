package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Contacto;

public interface ContactoRepository extends JpaRepository<Contacto, String> {
}
