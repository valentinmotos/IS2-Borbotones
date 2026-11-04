package com.dulcemayor.repository;

import com.dulcemayor.entity.Preventor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreventorRepository extends JpaRepository<Preventor, String> {

    Optional<Preventor> findByLegajoAndEliminadoFalse(String legajo);

    List<Preventor> findByEliminadoFalse();
}
