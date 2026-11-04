package com.dulcemayor.repository;

import com.dulcemayor.entity.Persona;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, String> {

    Optional<Persona> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);
}
