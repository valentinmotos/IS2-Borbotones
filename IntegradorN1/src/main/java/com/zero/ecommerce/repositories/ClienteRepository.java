package com.zero.ecommerce.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.zero.ecommerce.entities.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    Optional<Cliente> findByUsuarioIdAndEliminadoFalse(String usuarioId);
    Optional<Cliente> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);
}