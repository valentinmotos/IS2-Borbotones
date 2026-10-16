package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.services.BaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/** Endpoints CRUD comunes para los controladores REST de recursos. */
public abstract class BaseRestControllerTemplate<E> {

    protected final BaseService<E> baseService;

    protected BaseRestControllerTemplate(BaseService<E> baseService) {
        this.baseService = baseService;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleServiceException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @GetMapping
    public ResponseEntity<List<E>> listarActivos() throws Exception {
        return ResponseEntity.ok(baseService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<E> obtener(@PathVariable long id) throws Exception {
        return baseService.findOptionalById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<E> alta(@RequestBody E entity) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(baseService.saveOne(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<E> modificar(@PathVariable long id, @RequestBody E entity) throws Exception {
        try {
            return ResponseEntity.ok(baseService.updateOne(entity, id));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrada o inactiva")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> baja(@PathVariable long id) throws Exception {
        try {
            baseService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrada o inactiva")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}
