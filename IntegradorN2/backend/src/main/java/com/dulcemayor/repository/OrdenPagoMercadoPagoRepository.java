package com.dulcemayor.repository;

import com.dulcemayor.entity.OrdenPagoMercadoPago;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface OrdenPagoMercadoPagoRepository extends JpaRepository<OrdenPagoMercadoPago, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenPagoMercadoPago o where o.ordenExternaId = :id and o.eliminado = false")
    Optional<OrdenPagoMercadoPago> buscarPorOrdenExternaParaActualizar(@Param("id") String id);

    Optional<OrdenPagoMercadoPago> findFirstByFactura_IdAndEstadoInAndEliminadoFalseOrderByFechaCreacionDesc(
            String facturaId, Collection<String> estados);
}
