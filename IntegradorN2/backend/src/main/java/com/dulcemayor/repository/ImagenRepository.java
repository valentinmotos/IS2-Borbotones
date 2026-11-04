package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.Imagen;

public interface ImagenRepository extends JpaRepository<Imagen, String> {
}
