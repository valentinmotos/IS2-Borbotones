package com.example.servidor.dao;

import com.example.servidor.model.EnvioAutomatico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvioAutomaticoDAO extends JpaRepository<EnvioAutomatico, Long> {
    Optional<EnvioAutomatico> findByTipo(String tipo);
    boolean existsByTipo(String tipo);
}
