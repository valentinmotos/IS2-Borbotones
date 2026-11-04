package com.dulcemayor.repository;

import com.dulcemayor.entity.Minorista;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinoristaRepository extends JpaRepository<Minorista, String> {

    Optional<Minorista> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);

    List<Minorista> findByEliminadoFalse();
}
