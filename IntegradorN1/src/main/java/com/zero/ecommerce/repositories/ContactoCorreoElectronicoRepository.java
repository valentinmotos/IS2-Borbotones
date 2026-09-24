package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.ContactoCorreoElectronico;

public interface ContactoCorreoElectronicoRepository extends JpaRepository<ContactoCorreoElectronico, String> {
}
