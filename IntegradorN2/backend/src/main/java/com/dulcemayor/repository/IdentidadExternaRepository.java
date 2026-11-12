package com.dulcemayor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dulcemayor.entity.IdentidadExterna;

public interface IdentidadExternaRepository extends JpaRepository<IdentidadExterna, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<IdentidadExterna> findByEmisorAndSujeto(String emisor, String sujeto);

    Optional<IdentidadExterna> findByUsuario_IdAndEmisor(String usuarioId, String emisor);

    boolean existsByUsuario_IdAndEmisor(String usuarioId, String emisor);
}
