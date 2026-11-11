package com.dulcemayor.repository;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Los correos se guardan normalizados (sin espacios y en minúsculas, ver UsuarioService),
 * así que las búsquedas por nombre de usuario no necesitan IgnoreCase.
 */
public interface UsuarioRepository extends BaseRepository<Usuario> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByNombreUsuarioAndEliminadoFalse(String nombreUsuario);

    Optional<Usuario> findByIdAndEliminadoFalse(String id);

    /** Incluye los dados de baja: la restricción única de la tabla también los incluye. */
    boolean existsByNombreUsuario(String nombreUsuario);

    List<Usuario> findByEliminadoFalseAndRolInOrderByNombreUsuarioAsc(Collection<RolUsuario> roles);

    long countByRolAndActivoTrueAndEliminadoFalse(RolUsuario rol);
}
