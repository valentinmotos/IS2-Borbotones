package com.dulcemayor.repository;

import java.util.List;
import com.dulcemayor.entity.Mayorista;

public interface MayoristaRepository extends BaseRepository<Mayorista> {
    List<Mayorista> findByEliminadoFalse();
    boolean existsByCuitAndEliminadoFalse(String cuit);
    boolean existsByRazonSocialIgnoreCaseAndEliminadoFalse(String razonSocial);
}
