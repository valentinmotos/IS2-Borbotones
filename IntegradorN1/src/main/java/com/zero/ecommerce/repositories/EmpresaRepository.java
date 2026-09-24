package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, String> {
}
