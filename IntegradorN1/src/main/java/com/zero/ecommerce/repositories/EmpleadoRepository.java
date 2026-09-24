package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, String> {
}
