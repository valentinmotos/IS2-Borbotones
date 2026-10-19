package com.example.servidor.service;

import com.example.servidor.model.EntidadBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class BaseService<E extends EntidadBase> {
    protected final JpaRepository<E, Long> dao;
    private final String mensajeNoEncontrado;

    protected BaseService(JpaRepository<E, Long> dao, String mensajeNoEncontrado) {
        this.dao = dao;
        this.mensajeNoEncontrado = mensajeNoEncontrado;
    }

    public List<E> listar() {
        return dao.findAll();
    }

    public E buscar(Long id) {
        return dao.findById(id).orElseThrow(() -> new IllegalArgumentException(mensajeNoEncontrado));
    }

    public final E guardar(E entidad) {
        completarRelaciones(entidad);
        asignarValoresPorDefecto(entidad);
        return dao.save(entidad);
    }

    public void eliminar(Long id) {
        dao.deleteById(id);
    }

    protected void completarRelaciones(E entidad) {
    }

    protected void asignarValoresPorDefecto(E entidad) {
    }
}
