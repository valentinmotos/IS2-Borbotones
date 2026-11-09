package com.dulcemayor.repository;

import com.dulcemayor.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Los correos se guardan normalizados (sin espacios y en minúsculas, ver UsuarioService),
 * así que las búsquedas por nombre de usuario no necesitan IgnoreCase.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByNombreUsuarioAndEliminadoFalse(String nombreUsuario);

    Optional<Usuario> findByIdAndEliminadoFalse(String id);

    /** Incluye los dados de baja: la restricción única de la tabla también los incluye. */
    boolean existsByNombreUsuario(String nombreUsuario);
}
