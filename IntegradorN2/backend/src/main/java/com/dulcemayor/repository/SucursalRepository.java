package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.Sucursal;

public interface SucursalRepository extends JpaRepository<Sucursal, String> {
}
