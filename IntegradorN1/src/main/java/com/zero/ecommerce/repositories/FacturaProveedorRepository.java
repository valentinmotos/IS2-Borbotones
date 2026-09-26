package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.FacturaProveedor;
import com.zero.ecommerce.entities.enums.EstadoFactura;

public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, String> {
    List<FacturaProveedor> findAllByOrderByNumeroFacturaDesc();

    List<FacturaProveedor> findByEliminadoFalseOrderByNumeroFacturaDesc();

    List<FacturaProveedor> findByEstadoAndEliminadoFalseOrderByNumeroFacturaDesc(EstadoFactura estado);

    Optional<FacturaProveedor> findByIdAndEliminadoFalse(String id);

    /** La compra con el número más alto (incluidas las eliminadas), para la numeración secuencial. */
    Optional<FacturaProveedor> findFirstByOrderByNumeroFacturaDesc();
}
