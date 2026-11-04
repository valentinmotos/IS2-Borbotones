package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.PuntoDeVenta;

public interface PuntoDeVentaRepository extends JpaRepository<PuntoDeVenta, String> {
}
