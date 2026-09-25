package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FormaDePago;

public interface FormaDePagoRepository extends JpaRepository<FormaDePago, String> {
    List<FormaDePago> findAllByOrderByTipoPagoAscObservacionAsc();

    List<FormaDePago> findByEliminadoFalseOrderByTipoPagoAscObservacionAsc();

    Optional<FormaDePago> findByIdAndEliminadoFalse(String id);
}
