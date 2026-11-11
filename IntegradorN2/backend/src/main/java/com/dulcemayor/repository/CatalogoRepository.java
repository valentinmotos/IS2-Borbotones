package com.dulcemayor.repository;

import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.entity.Producto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas de lectura del catálogo público, incluido el stock y el precio vigentes. */
public interface CatalogoRepository extends JpaRepository<Producto, String> {

    @Query("""
            select new com.dulcemayor.dto.ProductoCatalogoDTO(p.id, p.nombre, p.descripcion,
                p.presentacion, p.unidadesPorBulto, vp.precio, c.id, c.nombre, i.id)
            from Producto p
            join p.categoria c
            left join p.imagen i
            join Stock s on s.producto = p and s.eliminado = false and s.cantidadActual > 0
            join VigenciaPrecio vp on vp.producto = p and vp.eliminado = false
                and vp.fechaDesde <= :hoy and (vp.fechaHasta is null or vp.fechaHasta >= :hoy)
            where p.eliminado = false
              and (:nombre = '' or locate(lower(:nombre), lower(p.nombre)) > 0)
              and (:categoriaId = '' or c.id = :categoriaId)
            order by p.nombre
            """)
    List<ProductoCatalogoDTO> buscarDisponibles(@Param("nombre") String nombre,
            @Param("categoriaId") String categoriaId, @Param("hoy") LocalDate hoy);
}
