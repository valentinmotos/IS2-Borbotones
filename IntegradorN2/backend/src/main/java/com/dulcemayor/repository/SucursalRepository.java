package com.dulcemayor.repository;

import java.util.List;
import com.dulcemayor.entity.Sucursal;

public interface SucursalRepository extends BaseRepository<Sucursal> {
    List<Sucursal> findByMayorista_IdAndEliminadoFalseOrderByNombreAsc(String idMayorista);
    boolean existsByMayorista_IdAndEliminadoFalse(String idMayorista);
    List<Sucursal> findByEliminadoFalse();
    boolean existsByImagen_IdAndEliminadoFalse(String idImagen);
}
