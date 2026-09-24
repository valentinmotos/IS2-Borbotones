package com.zero.ecommerce.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.NacionalidadRepository;

@Service
@Transactional(readOnly = true)
public class NacionalidadService {

    private final NacionalidadRepository repository;

    public NacionalidadService(NacionalidadRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void crearNacionalidad(String nombre) throws ErrorServiceException {
        validar(nombre);
        Nacionalidad nacionalidad = new Nacionalidad();
        nacionalidad.setNombre(nombre.strip());
        repository.save(nacionalidad);
    }

    public void validar(String nombre) throws ErrorServiceException {
        validar(nombre, null);
    }

    private void validar(String nombre, String idActual) throws ErrorServiceException {
        validarNombre(nombre);
        Optional<Nacionalidad> duplicada = encontrarPorNombre(nombre);
        if (duplicada.isPresent() && !duplicada.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe una nacionalidad con ese nombre.");
        }
    }

    private void validarNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre de la nacionalidad es obligatorio.");
        }
    }

    // SQLite no compara todas las letras Unicode al usar upper/lower. El catálogo es
    // pequeño: comparar en Java permite detectar también España / ESPAÑA.
    private Optional<Nacionalidad> encontrarPorNombre(String nombre) {
        return repository.findAllByOrderByNombreAsc().stream()
                .filter(n -> n.getNombre() != null && n.getNombre().strip().equalsIgnoreCase(nombre.strip()))
                .findFirst();
    }

    public Nacionalidad buscarNacionalidad(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La nacionalidad no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La nacionalidad no existe o fue eliminada."));
    }

    public Nacionalidad buscarNacionalidadPorNombre(String nombre) throws ErrorServiceException {
        validarNombre(nombre);
        return encontrarPorNombre(nombre).filter(n -> !n.isEliminado())
                .orElseThrow(() -> new ErrorServiceException("La nacionalidad no existe o fue eliminada."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarNacionalidad(String id, String nombre) throws ErrorServiceException {
        Nacionalidad nacionalidad = buscarNacionalidad(id);
        validar(nombre, id);
        nacionalidad.setNombre(nombre.strip());
        repository.save(nacionalidad);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarNacionalidad(String id) throws ErrorServiceException {
        Nacionalidad nacionalidad = buscarNacionalidad(id);
        nacionalidad.setEliminado(true);
        repository.save(nacionalidad);
    }

    public List<Nacionalidad> listarNacionalidad() {
        return repository.findAllByOrderByNombreAsc();
    }

    public List<Nacionalidad> listarNacionalidadActiva() {
        return repository.findByEliminadoFalseOrderByNombreAsc();
    }
}
