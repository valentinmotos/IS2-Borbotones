package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.Localidad;

public interface LocalidadRepository extends JpaRepository<Localidad, String> {
}
