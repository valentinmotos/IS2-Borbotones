package com.dulcemayor.repository;

import com.dulcemayor.entity.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, String> {
    Optional<Promocion> findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
            String codigo, LocalDate desde, LocalDate hasta);
    List<Promocion> findByEliminadoFalse();
}
