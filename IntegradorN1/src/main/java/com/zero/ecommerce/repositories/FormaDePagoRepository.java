package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FormaDePago;

public interface FormaDePagoRepository extends JpaRepository<FormaDePago, String> {
}
