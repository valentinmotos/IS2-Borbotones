package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Persona;
import com.zero.ecommerce.entities.enums.TipoDocumento;

public interface PersonaRepository extends JpaRepository<Persona, String> {

    Optional<Persona> findFirstByUsuario_NombreUsuarioIgnoreCaseAndEliminadoFalse(String nombreUsuario);

    // Documento único entre todas las personas activas (clientes y empleados).
    List<Persona> findByTipoDocumentoAndNumeroDocumentoAndEliminadoFalse(TipoDocumento tipoDocumento,
            String numeroDocumento);
}
