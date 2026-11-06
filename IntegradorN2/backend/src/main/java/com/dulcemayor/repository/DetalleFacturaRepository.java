package com.dulcemayor.repository;

import com.dulcemayor.entity.DetalleFactura;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleFacturaRepository extends JpaRepository<DetalleFactura, String> {
    List<DetalleFactura> findByFactura_IdAndEliminadoFalse(String facturaId);
}
