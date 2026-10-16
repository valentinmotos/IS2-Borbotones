package com.borbotones.videojuegos.services;

import java.util.List;
import java.util.Optional;

/**
 * Plantilla CRUD: concentra el acceso al repositorio y deja los pasos
 * específicos de cada entidad en hooks sobrescribibles.
 */
public abstract class BaseService<E> {

    public List<E> findAll() throws Exception {
        try {
            return listarActivos();
        } catch (Exception e) {
            throw new Exception("Error al listar: " + e.getMessage(), e);
        }
    }

    public Optional<E> findOptionalById(long id) throws Exception {
        try {
            return buscarPorId(id).filter(this::esActivo);
        } catch (Exception e) {
            throw new Exception("Error al obtener: " + e.getMessage(), e);
        }
    }

    public E findById(long id) throws Exception {
        return findOptionalById(id).orElseThrow(() -> new Exception("Entidad no encontrada o inactiva."));
    }

    public E saveOne(E entity) throws Exception {
        try {
            validar(BaseUseCaseService.ALTA, entity);
            preAlta(entity);
            E guardado = guardar(entity);
            postAlta(guardado);
            return guardado;
        } catch (Exception e) {
            throw new Exception("Error al dar de alta: " + e.getMessage(), e);
        }
    }

    public E updateOne(E entity, long id) throws Exception {
        try {
            Optional<E> existente = buscarPorId(id).filter(this::esActivo);
            if (existente.isEmpty()) {
                throw new Exception("Entidad no encontrada o inactiva.");
            }
            asignarId(entity, id);
            validar(BaseUseCaseService.MODIFICACION, entity);
            preModificacion(entity);
            E actualizado = guardar(entity);
            postModificacion(actualizado);
            return actualizado;
        } catch (Exception e) {
            throw new Exception("Error al modificar: " + e.getMessage(), e);
        }
    }

    public boolean deleteById(long id) throws Exception {
        try {
            E entidad = buscarPorId(id).filter(this::esActivo)
                    .orElseThrow(() -> new Exception("Entidad no encontrada o inactiva."));
            preBaja(entidad);
            validar(BaseUseCaseService.BAJA, entidad);
            cambiarEstadoActivo(entidad, false);
            guardar(entidad);
            postBaja(entidad);
            return true;
        } catch (Exception e) {
            throw new Exception("Error al dar de baja: " + e.getMessage(), e);
        }
    }

    protected abstract List<E> listarActivos();
    protected abstract Optional<E> buscarPorId(long id);
    protected abstract E guardar(E entity);
    protected abstract void asignarId(E entity, long id);
    protected abstract boolean esActivo(E entity);
    protected abstract void cambiarEstadoActivo(E entity, boolean activo);

    protected void validar(BaseUseCaseService useCase, E entity) throws Exception {}
    protected void preAlta(E entity) throws Exception {}
    protected void postAlta(E entity) throws Exception {}
    protected void preModificacion(E entity) throws Exception {}
    protected void postModificacion(E entity) throws Exception {}
    protected void preBaja(E entity) throws Exception {}
    protected void postBaja(E entity) throws Exception {}
}
