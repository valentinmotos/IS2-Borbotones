package com.dulcemayor.service;

import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.exception.ErrorServiceException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E5-02: consultas del perfil del distribuidor a partir del usuario del token (igual que
 * {@link PreventorService}). El alta del perfil la hace el ABM de usuarios internos (E2-05).
 */
@Service
@Transactional(readOnly = true)
public class DistribuidorService {

    private final DistribuidorRepository distribuidorRepository;

    public DistribuidorService(DistribuidorRepository distribuidorRepository) {
        this.distribuidorRepository = distribuidorRepository;
    }

    /** El distribuidor del usuario autenticado. Si el usuario no tiene perfil de distribuidor, se rechaza. */
    public Distribuidor buscarDistribuidorPorUsuario(String idUsuario) {
        return exigirPerfil(idUsuario == null ? Optional.empty()
                : distribuidorRepository.findByUsuario_IdAndEliminadoFalse(idUsuario));
    }

    /** Se llama dentro de las transacciones del reparto para que dos pedidos simultáneos no creen dos rutas. */
    @Transactional
    public Distribuidor bloquearPerfilParaReparto(String idUsuario) {
        return exigirPerfil(idUsuario == null ? Optional.empty()
                : distribuidorRepository.findParaRepartoByUsuario_IdAndEliminadoFalse(idUsuario));
    }

    private Distribuidor exigirPerfil(Optional<Distribuidor> distribuidor) {
        return distribuidor.orElseThrow(() -> new ErrorServiceException(
                "Tu usuario no tiene un perfil de distribuidor asociado. Pedile al Jefe que lo complete."));
    }
}
