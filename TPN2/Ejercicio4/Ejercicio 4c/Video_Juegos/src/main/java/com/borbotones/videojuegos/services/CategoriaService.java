package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.repositories.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService extends BaseService<Categoria> {

    private final CategoriaRepository repositorio;

    public CategoriaService(CategoriaRepository repositorio) { this.repositorio = repositorio; }

    @Override
    protected List<Categoria> listarActivos() {
        return repositorio.findAll().stream().filter(Categoria::isActivo).toList();
    }

    @Override protected Optional<Categoria> buscarPorId(long id) { return repositorio.findById(id); }
    @Override protected Categoria guardar(Categoria entity) { return repositorio.save(entity); }
    @Override protected void asignarId(Categoria entity, long id) { entity.setId(id); }
    @Override protected boolean esActivo(Categoria entity) { return entity.isActivo(); }
    @Override protected void cambiarEstadoActivo(Categoria entity, boolean activo) { entity.setActivo(activo); }
}
