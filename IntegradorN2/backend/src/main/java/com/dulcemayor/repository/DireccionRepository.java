package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.Direccion;

public interface DireccionRepository extends JpaRepository<Direccion, String> {
}
