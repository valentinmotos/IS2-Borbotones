package com.example.servidor.controller;

import com.example.servidor.model.EntidadBase;
import com.example.servidor.service.BaseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public abstract class BaseController<E extends EntidadBase> {
    protected final BaseService<E> service;

    protected BaseController(BaseService<E> service) {
        this.service = service;
    }

    @GetMapping
    public List<E> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public E buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public E crear(@RequestBody E entidad) {
        return guardar(null, entidad);
    }

    @PutMapping("/{id}")
    public E actualizar(@PathVariable Long id, @RequestBody E entidad) {
        return guardar(id, entidad);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    private E guardar(Long id, E entidad) {
        entidad.setId(id);
        return service.guardar(entidad);
    }
}
