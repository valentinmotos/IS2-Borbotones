package com.dulcemayor.repository;

import com.dulcemayor.entity.CotizacionDolar;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionDolarRepository extends JpaRepository<CotizacionDolar, String> {
    Optional<CotizacionDolar> findFirstByOrderByFechaDesc();
    List<CotizacionDolar> findAllByOrderByFechaDesc();
    Optional<CotizacionDolar> findFirstByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta);

    default Optional<CotizacionDolar> findByFecha(LocalDate fecha) {
        return findFirstByFechaBetweenOrderByFechaDesc(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }
}
