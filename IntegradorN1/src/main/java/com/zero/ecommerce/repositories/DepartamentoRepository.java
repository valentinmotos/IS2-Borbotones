package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Departamento;

public interface DepartamentoRepository extends JpaRepository<Departamento, String> {
}
