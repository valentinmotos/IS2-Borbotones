package com.dulcemayor.repository;

import java.util.List;
import com.dulcemayor.entity.FormaDePago;

public interface FormaDePagoRepository extends BaseRepository<FormaDePago> {
    List<FormaDePago> findByEliminadoFalse();
}
