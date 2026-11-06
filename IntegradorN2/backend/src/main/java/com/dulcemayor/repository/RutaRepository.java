package com.dulcemayor.repository;

import com.dulcemayor.entity.Ruta;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaRepository extends JpaRepository<Ruta, String> {
    List<Ruta> findByFechaAndEliminadoFalse(LocalDate fecha);
    List<Ruta> findByPreventor_IdAndEliminadoFalse(String preventorId);
    List<Ruta> findByDistribuidor_IdAndEliminadoFalse(String distribuidorId);
}
