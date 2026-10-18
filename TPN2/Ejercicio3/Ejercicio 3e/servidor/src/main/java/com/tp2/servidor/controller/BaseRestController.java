package com.tp2.servidor.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tp2.servidor.service.BaseRestService;

public abstract class BaseRestController<T, R, ID> {

    protected final BaseRestService<T, R, ID> baseService;

    protected BaseRestController(BaseRestService<T, R, ID> baseService) {
        this.baseService = baseService;
    }

    @GetMapping
    public final ResponseEntity<List<T>> listar() {
        return ResponseEntity.ok(baseService.listar());
    }

    @GetMapping("/{id}")
    public final ResponseEntity<T> obtener(@PathVariable ID id) {
        return baseService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public final ResponseEntity<T> crear(@RequestBody R request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baseService.crear(request));
    }

    @PutMapping("/{id}")
    public final ResponseEntity<T> modificar(@PathVariable ID id, @RequestBody R request) {
        return modificarRecurso(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public final ResponseEntity<Void> eliminar(@PathVariable ID id) {
        eliminarRecurso(id);
        return ResponseEntity.noContent().build();
    }

    protected Optional<T> modificarRecurso(ID id, R request) {
        return baseService.modificar(id, request);
    }

    protected void eliminarRecurso(ID id) {
        baseService.eliminar(id);
    }
}
