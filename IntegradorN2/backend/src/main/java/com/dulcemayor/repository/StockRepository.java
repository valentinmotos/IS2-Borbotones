package com.dulcemayor.repository;

import com.dulcemayor.dto.StockResponse;
import com.dulcemayor.entity.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findByProducto_Id(String productoId);

    /** E2-02: saldo activo del producto (vacío si nunca tuvo movimientos). */
    Optional<Stock> findByProducto_IdAndEliminadoFalse(String productoId);

    /** E2-02: saldo de todos los productos activos en una sola consulta; 0 si no tienen stock. */
    @Query("""
            select new com.dulcemayor.dto.StockResponse(p.id, p.codigo, p.nombre, p.presentacion, s.cantidadActual)
            from Producto p left join Stock s on s.producto = p and s.eliminado = false
            where p.eliminado = false
            order by p.nombre, p.codigo
            """)
    List<StockResponse> listarStock();
}
