package com.dulcemayor.repository;

import com.dulcemayor.entity.Distribuidor;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DistribuidorRepository extends JpaRepository<Distribuidor, String> {

    Optional<Distribuidor> findByLegajoAndEliminadoFalse(String legajo);

    List<Distribuidor> findByEliminadoFalse();

    /** E5-02: el distribuidor que corresponde al usuario del token. */
    Optional<Distribuidor> findByUsuario_IdAndEliminadoFalse(String usuarioId);

    /** E5-02: lo mismo, bloqueando el perfil para serializar las operaciones sobre su ruta del día. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Distribuidor> findParaRepartoByUsuario_IdAndEliminadoFalse(String usuarioId);
}
