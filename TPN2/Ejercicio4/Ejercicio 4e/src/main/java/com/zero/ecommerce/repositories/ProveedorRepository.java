package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, String> {
    List<Proveedor> findAllByOrderByRazonSocialAsc();

    List<Proveedor> findByEliminadoFalseOrderByRazonSocialAsc();

    Optional<Proveedor> findByIdAndEliminadoFalse(String id);

    Optional<Proveedor> findByRazonSocialIgnoreCaseAndEliminadoFalse(String razonSocial);

    List<Proveedor> findByRazonSocialContainingIgnoreCaseAndEliminadoFalseOrderByRazonSocialAsc(String razonSocial);
}
