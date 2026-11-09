package com.dulcemayor.repository;

import com.dulcemayor.entity.Pago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, String> {
    List<Pago> findByFactura_IdAndEliminadoFalse(String facturaId);
}
