package com.dulcemayor.repository;

import com.dulcemayor.entity.Distribuidor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistribuidorRepository extends JpaRepository<Distribuidor, String> {

    Optional<Distribuidor> findByLegajoAndEliminadoFalse(String legajo);

    List<Distribuidor> findByEliminadoFalse();
}
