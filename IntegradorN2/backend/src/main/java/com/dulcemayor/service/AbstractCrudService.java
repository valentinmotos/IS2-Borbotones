package com.dulcemayor.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.mapper.CrudMapper;
import com.dulcemayor.entity.BaseEntity;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.BaseRepository;

/**
 * Patrón Template Method (E0-08) · participante: clase abstracta.
 *
 * Fija el algoritmo de las operaciones de un ABM y deja ganchos que cada service concreto
 * redefine (participante: clase concreta, p. ej. CategoriaService):
 * <ul>
 *   <li>Alta: validar → nuevaEntidad → asignar → guardar → despuesDeGuardar → DTO.</li>
 *   <li>Modificación: buscar → validar → asignar → guardar → despuesDeGuardar → DTO.</li>
 *   <li>Baja lógica: buscar → antesDeEliminar → eliminado = true.</li>
 * </ul>
 * Los algoritmos son {@code protected final}; los métodos públicos no son final para que el proxy
 * transaccional de Spring pueda interceptarlos (mismo esquema que CrudTemplate de TP2 Ej. 4g).
 *
 * Recibe y devuelve DTOs: los controllers nunca ven entidades. buscarEntidad(id) queda público
 * para que otros services obtengan la entidad (p. ej. ProductoService pide la Categoria).
 *
 * @param <E> entidad (extiende BaseEntity: id UUID y baja lógica)
 * @param <Q> DTO de request
 * @param <S> DTO de respuesta
 */
@Transactional(readOnly = true)
public abstract class AbstractCrudService<E extends BaseEntity, Q, S> {

    protected final BaseRepository<E> repositorio;
    protected final CrudMapper<E, Q, S> mapper;

    protected AbstractCrudService(BaseRepository<E> repositorio, CrudMapper<E, Q, S> mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    // ---- Operaciones públicas (transaccionales) ----

    @Transactional
    public S crear(Q request) {
        return ejecutarAlta(request);
    }

    @Transactional
    public S modificar(String id, Q request) {
        return ejecutarModificacion(id, request);
    }

    @Transactional
    public void eliminar(String id) {
        ejecutarBaja(id);
    }

    public S buscar(String id) {
        return mapper.aRespuesta(buscarEntidad(id));
    }

    public List<S> listarActivos() {
        return mapper.aRespuestas(repositorio.findByEliminadoFalse(orden()));
    }

    /** Entidad activa por id, para uso de otros services. Lanza 404 si no existe o está dada de baja. */
    public E buscarEntidad(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException(mensajeNoEncontrado());
        }
        return repositorio.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(mensajeNoEncontrado()));
    }

    // ---- Métodos plantilla (el orden de los pasos no se puede cambiar) ----

    protected final S ejecutarAlta(Q request) {
        validar(request, null);
        E entidad = nuevaEntidad();
        asignar(entidad, request);
        E guardada = repositorio.save(entidad);
        despuesDeGuardar(guardada);
        return mapper.aRespuesta(guardada);
    }

    protected final S ejecutarModificacion(String id, Q request) {
        E entidad = buscarEntidad(id);
        validar(request, id);
        asignar(entidad, request);
        E guardada = repositorio.save(entidad);
        despuesDeGuardar(guardada);
        return mapper.aRespuesta(guardada);
    }

    protected final void ejecutarBaja(String id) {
        E entidad = buscarEntidad(id);
        antesDeEliminar(entidad);
        entidad.setEliminado(true);
        repositorio.save(entidad);
    }

    // ---- Ganchos obligatorios ----

    /**
     * Reglas de negocio del alta y la modificación. Lanza ErrorServiceException (400) o
     * DuplicadoException (409) con mensaje en español y el campo que falló.
     *
     * @param idActual null en el alta; en la modificación, el id del registro (para no
     *                 marcarlo como duplicado de sí mismo)
     */
    protected abstract void validar(Q request, String idActual);

    /** Instancia vacía de la entidad (normalmente {@code new Entidad()}). */
    protected abstract E nuevaEntidad();

    /** Mensaje del 404, p. ej. "No se encontró la categoría." */
    protected abstract String mensajeNoEncontrado();

    // ---- Ganchos opcionales ----

    /** Copia el request sobre la entidad. Por defecto usa el mapper; redefinir para normalizar o resolver relaciones. */
    protected void asignar(E entidad, Q request) {
        mapper.actualizar(request, entidad);
    }

    /** Reglas que impiden la baja (p. ej. tener hijos activos). Por defecto, ninguna. */
    protected void antesDeEliminar(E entidad) {
    }

    /** Acción posterior al alta o la modificación (p. ej. registrar un movimiento). Por defecto, ninguna. */
    protected void despuesDeGuardar(E entidad) {
    }

    /** Orden del listado de activos. Por defecto, por id. */
    protected Sort orden() {
        return Sort.by("id");
    }
}
