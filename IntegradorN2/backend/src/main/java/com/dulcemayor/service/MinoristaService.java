package com.dulcemayor.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.MinoristaOpcion;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.MinoristaRepository;

/** E2-06: consultas de minoristas que necesitan los puntos de venta. El registro es de E2-04. */
@Service
@Transactional(readOnly = true)
public class MinoristaService {

    private static final String MINORISTA_NO_ENCONTRADO = "No se encontró el minorista.";

    private final MinoristaRepository minoristaRepository;

    public MinoristaService(MinoristaRepository minoristaRepository) {
        this.minoristaRepository = minoristaRepository;
    }

    /** Minoristas activos ordenados por apellido y nombre. */
    public List<MinoristaOpcion> listarMinoristaActivo() {
        return minoristaRepository.findByEliminadoFalseOrderByApellidoAscNombreAsc().stream()
                .map(m -> new MinoristaOpcion(m.getId(), m.getNombre(), m.getApellido(), m.getNumeroDocumento()))
                .toList();
    }

    /** Para otros services: el minorista activo o 404. */
    public Minorista buscarEntidad(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException(MINORISTA_NO_ENCONTRADO);
        }
        return minoristaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(MINORISTA_NO_ENCONTRADO));
    }
}
