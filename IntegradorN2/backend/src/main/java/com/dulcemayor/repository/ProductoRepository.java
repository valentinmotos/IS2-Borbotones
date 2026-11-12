package com.dulcemayor.repository;

import com.dulcemayor.entity.Producto;
import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.dto.ProductoResponse;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends BaseRepository<Producto> {
    Optional<Producto> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, String id);
    List<Producto> findByEliminadoFalse();
    List<Producto> findByEliminadoFalseAndEnOfertaTrue();

    /** E0-08: una categoría con productos activos no se puede dar de baja. */
    boolean existsByCategoria_IdAndEliminadoFalse(String idCategoria);

    @Query("select p from Producto p join Stock s on s.producto = p " +
            "where p.eliminado = false and s.eliminado = false and s.cantidadActual > 0")
    List<Producto> findProductosCatalogo();

    @Query("""
            select new com.dulcemayor.dto.ProductoResponse(p.id, p.codigo, p.nombre, p.descripcion,
                p.presentacion, p.unidadesPorBulto, c.id, c.nombre, i.id, p.enOferta)
            from Producto p join p.categoria c left join p.imagen i
            where p.eliminado = false
              and (:categoria = '' or c.id = :categoria)
              and (:texto = '' or locate(lower(:texto), lower(p.codigo)) > 0
                    or locate(lower(:texto), lower(p.nombre)) > 0)
            order by p.nombre, p.codigo
            """)
    List<ProductoResponse> buscarActivos(String texto, String categoria);

    /**
     * E3-01: catálogo público. Solo productos activos con stock mayor que cero y precio vigente hoy.
     * Texto, categoría e id vacíos = sin filtro (el id lo usa el detalle de E3-03).
     */
    @Query("""
            select new com.dulcemayor.dto.ProductoCatalogoDTO(p.id, p.codigo, p.nombre, p.descripcion,
                p.presentacion, p.unidadesPorBulto, vp.precio, s.cantidadActual, c.id, c.nombre, i.id)
            from Producto p
            join p.categoria c
            left join p.imagen i
            join Stock s on s.producto = p and s.eliminado = false and s.cantidadActual > 0
            join VigenciaPrecio vp on vp.producto = p and vp.eliminado = false
                and vp.fechaDesde <= :hoy and (vp.fechaHasta is null or vp.fechaHasta >= :hoy)
            where p.eliminado = false
              and (:id = '' or p.id = :id)
              and (:soloOfertas = false or p.enOferta = true)
              and (:texto = '' or locate(lower(:texto), lower(p.nombre)) > 0)
              and (:categoria = '' or c.id = :categoria)
            order by p.nombre, p.codigo
            """)
    List<ProductoCatalogoDTO> buscarCatalogo(String id, String texto, String categoria, LocalDate hoy,
            boolean soloOfertas);

    default List<ProductoCatalogoDTO> buscarCatalogo(String id, String texto, String categoria, LocalDate hoy) {
        return buscarCatalogo(id, texto, categoria, hoy, false);
    }

    /** E2-03: producto activo con bloqueo de escritura, para cambiar su precio sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id and p.eliminado = false")
    Optional<Producto> buscarActivoParaActualizar(String id);
}
