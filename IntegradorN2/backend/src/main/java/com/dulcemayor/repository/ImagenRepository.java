package com.dulcemayor.repository;

import com.dulcemayor.entity.Imagen;
import com.dulcemayor.repository.BaseRepository;

public interface ImagenRepository extends BaseRepository<Imagen> {
    boolean existsByNombre(String nombre);
}
