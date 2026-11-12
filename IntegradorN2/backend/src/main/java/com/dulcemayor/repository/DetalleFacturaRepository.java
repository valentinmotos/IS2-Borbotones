package com.dulcemayor.repository;

import com.dulcemayor.dto.EstadisticasResponse.ProductoVendido;
import com.dulcemayor.entity.DetalleFactura;
import com.dulcemayor.entity.enums.EstadoFactura;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DetalleFacturaRepository extends JpaRepository<DetalleFactura, String> {
    List<DetalleFactura> findByFactura_IdAndEliminadoFalse(String facturaId);

    @Query("""
            select new com.dulcemayor.dto.EstadisticasResponse$ProductoVendido(
                p.id, p.codigo, p.nombre, sum(d.cantidad))
            from DetalleFactura d join d.factura f join d.producto p
            where d.eliminado = false and f.eliminado = false and f.estado <> :estadoExcluido
                and f.fechaFactura >= :desde and f.fechaFactura < :hastaExclusivo
            group by p.id, p.codigo, p.nombre
            order by sum(d.cantidad) desc, p.codigo, p.id
            """)
    List<ProductoVendido> listarMasVendidos(LocalDate desde, LocalDate hastaExclusivo,
            EstadoFactura estadoExcluido, Pageable pagina);
}
