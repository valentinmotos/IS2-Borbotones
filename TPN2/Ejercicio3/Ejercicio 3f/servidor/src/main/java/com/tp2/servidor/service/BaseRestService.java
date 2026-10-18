package com.tp2.servidor.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.data.jpa.repository.JpaRepository;

/** Template Method para las operaciones REST comunes de los servicios. */
public abstract class BaseRestService<T, R, ID> {
    protected final JpaRepository<T, ID> repository;
    private final Function<R, T> adaptador;
    private final Predicate<T> activo;

    protected BaseRestService(JpaRepository<T, ID> repository, Function<R, T> adaptador, Predicate<T> activo) {
        this.repository = repository;
        this.adaptador = adaptador;
        this.activo = activo;
    }

    public final List<T> listar() {
        return repository.findAll().stream().filter(activo).toList();
    }

    public final Optional<T> buscarPorId(ID id) {
        return repository.findById(id).filter(activo);
    }

    public final T buscarActivo(ID id) {
        return buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Entidad no encontrada o eliminada"));
    }

    public final T crear(R request) {
        T entidad = adaptador.apply(request);
        preAlta(entidad);
        validar(Operacion.ALTA, entidad);
        T guardada = repository.save(entidad);
        postAlta(guardada);
        return guardada;
    }

    public final Optional<T> modificar(ID id, R request) {
        T entidad = adaptador.apply(request);
        preModificacion(entidad);
        validar(Operacion.MODIFICACION, entidad);
        return buscarPorId(id).map(existente -> {
            actualizar(existente, entidad);
            return repository.save(existente);
        });
    }

    public final void eliminar(ID id) {
        T entidad = buscarActivo(id);
        preBaja(entidad);
        validar(Operacion.BAJA, entidad);
        marcarEliminada(entidad);
        repository.save(entidad);
    }

    protected void validar(Operacion operacion, T entidad) { }
    protected void preAlta(T entidad) { }
    protected void postAlta(T entidad) { }
    protected void preModificacion(T entidad) { }
    protected void preBaja(T entidad) { }

    protected abstract void actualizar(T existente, T nueva);

    protected abstract void marcarEliminada(T entidad);

    public enum Operacion { ALTA, MODIFICACION, BAJA }
}
