package com.dulcemayor.repository;

import com.dulcemayor.entity.Persona;
import com.dulcemayor.entity.enums.TipoDocumento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, String> {

    Optional<Persona> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    Optional<Persona> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
