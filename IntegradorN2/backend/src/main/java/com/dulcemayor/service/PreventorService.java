package com.dulcemayor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.entity.Preventor;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.PreventorRepository;
import java.util.Optional;

/** E2-06: consultas de preventores. El alta del perfil la hace el ABM de usuarios internos (E2-05). */
@Service
@Transactional(readOnly = true)
public class PreventorService {

    private final PreventorRepository preventorRepository;

    public PreventorService(PreventorRepository preventorRepository) {
        this.preventorRepository = preventorRepository;
    }

    /** El preventor del usuario autenticado. Si el usuario no tiene perfil de preventor, se rechaza. */
    public Preventor buscarPreventorPorUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            throw new ErrorServiceException("Solo un preventor puede dar de alta puntos de venta.");
        }
        return exigirPerfil(preventorRepository.findByUsuario_IdAndEliminadoFalse(idUsuario));
    }

    /** Se llama dentro de la transacción de carga para serializar reintentos del mismo preventor. */
    @Transactional
    public Preventor bloquearPerfilParaCarga(String idUsuario) {
        return exigirPerfil(preventorRepository.findParaCargaByUsuario_IdAndEliminadoFalse(idUsuario));
    }

    private Preventor exigirPerfil(Optional<Preventor> preventor) {
        return preventor.orElseThrow(() -> new ErrorServiceException(
                "Tu usuario no tiene un perfil de preventor asociado. Pedile al Jefe que lo complete."));
    }
}
