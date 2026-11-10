package com.dulcemayor.repository;

import com.dulcemayor.entity.Producto;
import com.dulcemayor.dto.ProductoResponse;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

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
                p.presentacion, p.unidadesPorBulto, c.id, c.nombre, i.id)
            from Producto p join p.categoria c left join p.imagen i
            where p.eliminado = false
              and (:categoria = '' or c.id = :categoria)
              and (:texto = '' or locate(lower(:texto), lower(p.codigo)) > 0
                    or locate(lower(:texto), lower(p.nombre)) > 0)
            order by p.nombre, p.codigo
            """)
    List<ProductoResponse> buscarActivos(String texto, String categoria);

    /** E2-03: producto activo con bloqueo de escritura, para cambiar su precio sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id and p.eliminado = false")
    Optional<Producto> buscarActivoParaActualizar(String id);
}
