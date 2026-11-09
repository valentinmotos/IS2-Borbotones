package com.dulcemayor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.dulcemayor.service.AbstractCrudService;

import jakarta.validation.Valid;

/**
 * Patrón Template Method (E0-08) · participante: clase abstracta del lado REST.
 *
 * Endpoints comunes de todo ABM, que solo delegan en el service (los controllers no tienen lógica
 * ni capturan excepciones: los errores los traduce GlobalExceptionHandler a {mensaje, campo}).
 * <pre>
 * GET    /         listado de activos        200
 * GET    /{id}     uno por id                200 · 404
 * POST   /         alta                      201 · 400 · 409
 * PUT    /{id}     modificación              200 · 400 · 404 · 409
 * DELETE /{id}     baja lógica               204 · 400 · 404
 * </pre>
 * El controller concreto solo define @RestController, @RequestMapping("/api/...") y su service,
 * y puede sumar endpoints propios (p. ej. consultas en cascada).
 *
 * @param <Q> DTO de request
 * @param <S> DTO de respuesta
 */
public abstract class AbstractCrudController<Q, S> {

    protected final AbstractCrudService<?, Q, S> servicio;

    protected AbstractCrudController(AbstractCrudService<?, Q, S> servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<S> listar() {
        return servicio.listarActivos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable String id) {
        return ResponseEntity.ok(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public S crear(@Valid @RequestBody Q request) {
        return servicio.crear(request);
    }

    @PutMapping("/{id}")
    public S modificar(@PathVariable String id, @Valid @RequestBody Q request) {
        return servicio.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
