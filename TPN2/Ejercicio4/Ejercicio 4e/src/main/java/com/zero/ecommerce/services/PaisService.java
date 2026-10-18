package com.zero.ecommerce.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.PaisRepository;
import com.zero.ecommerce.repositories.ProvinciaRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class PaisService {

    private final PaisRepository repository;
    private final ProvinciaRepository provinciaRepository;

    public PaisService(PaisRepository repository, ProvinciaRepository provinciaRepository) {
        this.repository = repository;
        this.provinciaRepository = provinciaRepository;
    }

    // Devuelve el país creado (el diagrama dice void) porque el seeder necesita su id.
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Pais crearPais(String nombre) throws ErrorServiceException {
        validar(nombre);
        Pais pais = new Pais();
        pais.setNombre(nombre.strip());
        return repository.save(pais);
    }

    public void validar(String nombre) throws ErrorServiceException {
        validar(nombre, null);
    }

    private void validar(String nombre, String idActual) throws ErrorServiceException {
        validarNombre(nombre);
        Optional<Pais> duplicado = encontrarActivoPorNombre(nombre);
        if (duplicado.isPresent() && !duplicado.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe un país con ese nombre.");
        }
    }

    private void validarNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre del país es obligatorio.");
        }
    }

    // Solo se comparan los activos: un país eliminado no se puede reactivar,
    // así que se permite volver a darlo de alta (igual que en FormaDePagoService).
    private Optional<Pais> encontrarActivoPorNombre(String nombre) {
        return repository.findByEliminadoFalseOrderByNombreAsc().stream()
                .filter(p -> TextoUtils.mismoNombre(p.getNombre(), nombre))
                .findFirst();
    }

    public Pais buscarPais(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El país no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El país no existe o fue eliminado."));
    }

    public Pais buscarPaisPorNombre(String nombre) throws ErrorServiceException {
        validarNombre(nombre);
        return encontrarActivoPorNombre(nombre)
                .orElseThrow(() -> new ErrorServiceException("El país no existe o fue eliminado."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarPais(String id, String nombre) throws ErrorServiceException {
        Pais pais = buscarPais(id);
        validar(nombre, id);
        pais.setNombre(nombre.strip());
        repository.save(pais);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarPais(String id) throws ErrorServiceException {
        Pais pais = buscarPais(id);
        if (provinciaRepository.existsByPais_IdAndEliminadoFalse(id)) {
            throw new ErrorServiceException("No se puede eliminar el país porque tiene provincias activas.");
        }
        pais.setEliminado(true);
        repository.save(pais);
    }

    public List<Pais> listarPais() {
        return repository.findAllByOrderByNombreAsc();
    }

    public List<Pais> listarPaisActivo() {
        return repository.findByEliminadoFalseOrderByNombreAsc();
    }

    /** Filas del listado del ABM. */
    public List<FilaTablaDTO> listarFilaPaisActivo() {
        return listarPaisActivo().stream()
                .map(p -> new FilaTablaDTO(p.getId(), p.getNombre(), List.of(p.getNombre())))
                .toList();
    }

    /** Opciones para los selects: id → nombre, ordenadas por nombre. */
    public Map<String, String> listarOpcionPaisActivo() {
        Map<String, String> opciones = new LinkedHashMap<>();
        listarPaisActivo().forEach(p -> opciones.put(p.getId(), p.getNombre()));
        return opciones;
    }
}
