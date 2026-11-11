package com.dulcemayor.repository;

import com.dulcemayor.entity.Preventor;
import java.util.List;
import java.util.Optional;

public interface PreventorRepository extends BaseRepository<Preventor> {

    Optional<Preventor> findByLegajoAndEliminadoFalse(String legajo);

    /** E2-06: el preventor que corresponde al usuario del token. */
    Optional<Preventor> findByUsuario_IdAndEliminadoFalse(String usuarioId);

    List<Preventor> findByEliminadoFalse();
}
