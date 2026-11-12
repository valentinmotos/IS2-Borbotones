package com.dulcemayor.repository;

import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.enums.RolUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface MinoristaRepository extends BaseRepository<Minorista> {

    Optional<Minorista> findByNumeroDocumentoAndEliminadoFalse(String numeroDocumento);

    Optional<Minorista> findByUsuario_NombreUsuarioAndEliminadoFalse(String nombreUsuario);

    List<Minorista> findByEliminadoFalse();

    /** E2-07: el minorista del usuario autenticado. */
    Optional<Minorista> findByUsuario_IdAndEliminadoFalse(String usuarioId);

    /** E2-06: minoristas activos para elegir como responsables de un punto de venta. */
    List<Minorista> findByEliminadoFalseOrderByApellidoAscNombreAsc();

    @Query("""
            select u.nombreUsuario from Minorista m join m.usuario u
            where m.eliminado = false and u.eliminado = false and u.activo = true and u.rol = :rol
            order by u.nombreUsuario
            """)
    List<String> listarCorreosActivos(RolUsuario rol);
}
