package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.repositories.EstudioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstudioService implements BaseService<Estudio> {
    private final EstudioRepository repositorio;

    public EstudioService(EstudioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Estudio> findAll() {
        return repositorio.findAll();
    }

    @Transactional(readOnly = true)
    public List<Estudio> findAllActivos() {
        return repositorio.findAllByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Estudio findById(long id) {
        validarId(id);
        return repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el estudio solicitado"));
    }

    @Override
    @Transactional
    public Estudio saveOne(Estudio entity) {
        validarNombre(entity);
        entity.setId(0);
        entity.setNombre(entity.getNombre().trim());
        entity.setActivo(true);
        return repositorio.save(entity);
    }

    @Override
    @Transactional
    public Estudio updateOne(Estudio entity, long id) {
        validarNombre(entity);
        Estudio existente = findById(id);
        existente.setNombre(entity.getNombre().trim());
        return repositorio.save(existente);
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        Estudio estudio = findById(id);
        estudio.setActivo(false);
        repositorio.save(estudio);
        return true;
    }

    private void validarNombre(Estudio estudio) {
        if (estudio == null || estudio.getNombre() == null) {
            throw new IllegalArgumentException("El nombre del estudio es obligatorio");
        }
        String nombre = estudio.getNombre().trim();
        if (nombre.length() < 2 || nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre del estudio debe tener entre 2 y 100 caracteres");
        }
    }

    private void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de estudio no es valido");
        }
    }
}
