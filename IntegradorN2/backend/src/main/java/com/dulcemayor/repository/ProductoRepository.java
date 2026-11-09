package com.dulcemayor.repository;

import com.dulcemayor.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, String> {
    Optional<Producto> findByCodigoIgnoreCase(String codigo);
    List<Producto> findByEliminadoFalse();
    List<Producto> findByEliminadoFalseAndEnOfertaTrue();

    /** E0-08: una categoría con productos activos no se puede dar de baja. */
    boolean existsByCategoria_IdAndEliminadoFalse(String idCategoria);

    @Query("select p from Producto p join Stock s on s.producto = p " +
            "where p.eliminado = false and s.eliminado = false and s.cantidadActual > 0")
    List<Producto> findProductosCatalogo();
}
