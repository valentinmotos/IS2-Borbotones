package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.repositories.EstudioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstudioService extends BaseService<Estudio> {

    private final EstudioRepository repositorio;

    public EstudioService(EstudioRepository repositorio) { this.repositorio = repositorio; }

    @Override
    protected List<Estudio> listarActivos() {
        return repositorio.findAll().stream().filter(Estudio::isActivo).toList();
    }
    @Override protected Optional<Estudio> buscarPorId(long id) { return repositorio.findById(id); }
    @Override protected Estudio guardar(Estudio entity) { return repositorio.save(entity); }
    @Override protected void asignarId(Estudio entity, long id) { entity.setId(id); }
    @Override protected boolean esActivo(Estudio entity) { return entity.isActivo(); }
    @Override protected void cambiarEstadoActivo(Estudio entity, boolean activo) { entity.setActivo(activo); }
}
