package com.dulcemayor.repository;

import com.dulcemayor.entity.Departamento;
import java.util.List;

public interface DepartamentoRepository extends BaseRepository<Departamento> {

    List<Departamento> findByEliminadoFalse();

    List<Departamento> findByEliminadoFalseOrderByNombreAsc();
}
