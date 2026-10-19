package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.ContactoTelefonico;

public interface ContactoTelefonicoRepository extends JpaRepository<ContactoTelefonico, String> {
    List<ContactoTelefonico> findAllByOrderByTelefonoAsc();

    List<ContactoTelefonico> findByEliminadoFalseOrderByTelefonoAsc();

    Optional<ContactoTelefonico> findByIdAndEliminadoFalse(String id);
}
