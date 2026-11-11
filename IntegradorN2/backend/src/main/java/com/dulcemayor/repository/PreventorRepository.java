package com.dulcemayor.repository;

import com.dulcemayor.entity.Preventor;
import java.util.List;
import java.util.Optional;
import com.dulcemayor.repository.BaseRepository;

public interface PreventorRepository extends BaseRepository<Preventor> {

    Optional<Preventor> findByLegajoAndEliminadoFalse(String legajo);
    Optional<Preventor> findByUsuario_IdAndEliminadoFalse(String usuarioId);

    List<Preventor> findByEliminadoFalse();
}
