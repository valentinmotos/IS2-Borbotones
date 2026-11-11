package com.dulcemayor.repository;

import com.dulcemayor.entity.Minorista;
import java.util.List;
import java.util.Optional;

public interface MinoristaRepository extends BaseRepository<Minorista> {

    Optional<Minorista> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);

    List<Minorista> findByEliminadoFalse();
}
