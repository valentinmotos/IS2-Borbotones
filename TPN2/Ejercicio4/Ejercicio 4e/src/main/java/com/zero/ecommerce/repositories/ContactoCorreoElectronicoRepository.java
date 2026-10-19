package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.ContactoCorreoElectronico;

public interface ContactoCorreoElectronicoRepository extends JpaRepository<ContactoCorreoElectronico, String> {
    List<ContactoCorreoElectronico> findAllByOrderByEmailAsc();

    List<ContactoCorreoElectronico> findByEliminadoFalseOrderByEmailAsc();

    Optional<ContactoCorreoElectronico> findByIdAndEliminadoFalse(String id);
}
