package com.dulcemayor.repository;

import com.dulcemayor.entity.Minorista;
import java.util.List;
import java.util.Optional;

public interface MinoristaRepository extends BaseRepository<Minorista> {

    Optional<Minorista> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);

    Optional<Minorista> findByUsuario_NombreUsuarioAndEliminadoFalse(String nombreUsuario);

    List<Minorista> findByEliminadoFalse();

    /** E2-06: minoristas activos para elegir como responsables de un punto de venta. */
    List<Minorista> findByEliminadoFalseOrderByApellidoAscNombreAsc();
}
