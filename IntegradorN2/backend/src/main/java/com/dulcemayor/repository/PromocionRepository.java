package com.dulcemayor.repository;

import com.dulcemayor.entity.Promocion;
import com.dulcemayor.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends BaseRepository<Promocion> {
    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, String id);

    Optional<Promocion> findByCodigoIgnoreCaseAndEliminadoFalseAndFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(
            String codigo, LocalDate desde, LocalDate hasta);

    List<Promocion> findByEliminadoFalseOrderByCodigoAsc();
}
