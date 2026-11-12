package com.dulcemayor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

import com.dulcemayor.entity.PuntoDeVenta;

/** E2-06: cada consulta trae el punto con su minorista y su dirección completa (sin N+1). */
public interface PuntoDeVentaRepository extends BaseRepository<PuntoDeVenta> {

    @Override
    @EntityGraph(attributePaths = { "minorista", "preventor", "direccion.localidad.departamento" })
    Optional<PuntoDeVenta> findByIdAndEliminadoFalse(String id);

    @EntityGraph(attributePaths = { "minorista", "preventor", "direccion.localidad.departamento" })
    List<PuntoDeVenta> findByEliminadoFalseOrderByNombreComercioAsc();

    @EntityGraph(attributePaths = { "minorista", "preventor", "direccion.localidad.departamento" })
    List<PuntoDeVenta> findByDireccion_Localidad_Departamento_IdAndEliminadoFalseOrderByNombreComercioAsc(
            String departamentoId);

    @EntityGraph(attributePaths = { "minorista", "preventor", "direccion.localidad.departamento" })
    List<PuntoDeVenta> findByPreventor_IdAndEliminadoFalseOrderByNombreComercioAsc(String preventorId);

    @EntityGraph(attributePaths = { "minorista", "preventor", "direccion.localidad.departamento" })
    List<PuntoDeVenta> findByMinorista_IdAndEliminadoFalseOrderByNombreComercioAsc(String minoristaId);

    boolean existsByMinorista_IdAndEliminadoFalse(String minoristaId);

    Optional<PuntoDeVenta> findByIdAndPreventor_IdAndEliminadoFalseAndMinorista_EliminadoFalse(
            String id, String preventorId);
}
