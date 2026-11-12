package com.dulcemayor.repository;

import com.dulcemayor.entity.Ruta;
import com.dulcemayor.entity.enums.RutaEstado;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaRepository extends JpaRepository<Ruta, String> {
    List<Ruta> findByFechaAndEliminadoFalse(LocalDate fecha);
    List<Ruta> findByPreventor_IdAndEliminadoFalse(String preventorId);
    List<Ruta> findByDistribuidor_IdAndEliminadoFalse(String distribuidorId);
    /** E5-01: la ruta del día de un preventor. */
    Optional<Ruta> findFirstByPreventor_IdAndFechaAndEliminadoFalse(String preventorId, LocalDate fecha);
    /** E5-02: la ruta de reparto del día de un distribuidor. */
    Optional<Ruta> findFirstByDistribuidor_IdAndFechaAndEliminadoFalse(String distribuidorId, LocalDate fecha);
    /** E5-02: rutas de reparto de días anteriores que quedaron abiertas. */
    List<Ruta> findByDistribuidor_IdAndFechaBeforeAndEstadoAndEliminadoFalse(String distribuidorId, LocalDate fecha,
            RutaEstado estado);
}
