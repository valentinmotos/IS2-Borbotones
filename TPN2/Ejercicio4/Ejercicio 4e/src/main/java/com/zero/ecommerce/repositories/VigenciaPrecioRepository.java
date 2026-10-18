package com.zero.ecommerce.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zero.ecommerce.entities.VigenciaPrecio;

public interface VigenciaPrecioRepository extends JpaRepository<VigenciaPrecio, String> {

    List<VigenciaPrecio> findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(String productoId);

    List<VigenciaPrecio> findByEliminadoFalseOrderByFechaDesdeAsc();

    List<VigenciaPrecio> findByEliminadoFalseAndFechaHastaIsNull();

    Optional<VigenciaPrecio> findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(String productoId);

    @Query("""
            select v from VigenciaPrecio v
            join fetch v.producto p
            join fetch p.subCategoria s
            join fetch s.categoria c
            where v.eliminado = false
              and v.fechaHasta is null
              and v.fechaDesde < :limite
              and p.eliminado = false
              and s.eliminado = false
              and c.eliminado = false
            order by c.nombre, s.nombre, p.nombre, p.talle
            """)
    List<VigenciaPrecio> listarVigenciasVencidas(@Param("limite") LocalDate limite);

    @Query("""
            select count(v) from VigenciaPrecio v
            join v.producto p
            join p.subCategoria s
            join s.categoria c
            where v.eliminado = false
              and v.fechaHasta is null
              and v.fechaDesde < :limite
              and p.eliminado = false
              and s.eliminado = false
              and c.eliminado = false
            """)
    long contarVigenciasVencidas(@Param("limite") LocalDate limite);
}
