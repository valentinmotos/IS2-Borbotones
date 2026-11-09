package com.dulcemayor.repository;

import com.dulcemayor.entity.Localidad;
import java.util.List;
import java.util.Optional;

public interface LocalidadRepository extends BaseRepository<Localidad> {

    List<Localidad> findByDepartamento_IdAndEliminadoFalseOrderByNombreAsc(String departamentoId);

    Optional<Localidad> findByCodigoPostalAndEliminadoFalse(String codigoPostal);
}
