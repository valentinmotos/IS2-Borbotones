package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.PuntoDeVenta;
import java.util.List;

public interface PuntoDeVentaRepository extends JpaRepository<PuntoDeVenta, String> {
    List<PuntoDeVenta> findByEliminadoFalseOrderByNombreComercioAsc();
    List<PuntoDeVenta> findByDireccion_Localidad_Departamento_IdAndEliminadoFalseOrderByNombreComercioAsc(String departamentoId);
    boolean existsByMinorista_IdAndEliminadoFalse(String minoristaId);
}
