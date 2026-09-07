package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.repositories.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService implements BaseService<Categoria> {
    private final CategoriaRepository repositorio;

    public CategoriaService(CategoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> findAll() {
        return repositorio.findAll();
    }

    @Transactional(readOnly = true)
    public List<Categoria> findAllActivas() {
        return repositorio.findAllByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria findById(long id) {
        validarId(id);
        return repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la categoria solicitada"));
    }

    @Override
    @Transactional
    public Categoria saveOne(Categoria entity) {
        validarNombre(entity);
        entity.setId(0);
        entity.setNombre(entity.getNombre().trim());
        entity.setActivo(true);
        return repositorio.save(entity);
    }

    @Override
    @Transactional
    public Categoria updateOne(Categoria entity, long id) {
        validarNombre(entity);
        Categoria existente = findById(id);
        existente.setNombre(entity.getNombre().trim());
        return repositorio.save(existente);
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        Categoria categoria = findById(id);
        categoria.setActivo(false);
        repositorio.save(categoria);
        return true;
    }

    private void validarNombre(Categoria categoria) {
        if (categoria == null || categoria.getNombre() == null) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio");
        }
        String nombre = categoria.getNombre().trim();
        if (nombre.length() < 2 || nombre.length() > 80) {
            throw new IllegalArgumentException("El nombre de la categoria debe tener entre 2 y 80 caracteres");
        }
    }

    private void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de categoria no es valido");
        }
    }
}
