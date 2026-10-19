package com.zero.ecommerce.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.ConfiguracionCorreoEmpresa;

public interface ConfiguracionCorreoEmpresaRepository extends JpaRepository<ConfiguracionCorreoEmpresa, String> {
    Optional<ConfiguracionCorreoEmpresa> findByIdAndEliminadoFalse(String id);

    Optional<ConfiguracionCorreoEmpresa> findByEmpresa_IdAndEliminadoFalse(String empresaId);
}
