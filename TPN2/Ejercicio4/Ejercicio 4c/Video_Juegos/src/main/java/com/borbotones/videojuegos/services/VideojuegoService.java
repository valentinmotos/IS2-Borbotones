package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.repositories.VideojuegoRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class VideojuegoService extends BaseService<Videojuego> {

    private final VideojuegoRepository repositorio;

    public VideojuegoService(VideojuegoRepository repositorio) { this.repositorio = repositorio; }

    @Override protected List<Videojuego> listarActivos() { return repositorio.findAllByActivo(); }
    @Override protected Optional<Videojuego> buscarPorId(long id) { return repositorio.findById(id); }
    @Override protected Videojuego guardar(Videojuego entity) { return repositorio.save(entity); }
    @Override protected void asignarId(Videojuego entity, long id) { entity.setId(id); }
    @Override protected boolean esActivo(Videojuego entity) { return entity.isActivo(); }
    @Override protected void cambiarEstadoActivo(Videojuego entity, boolean activo) { entity.setActivo(activo); }

    public List<Videojuego> findAllByActivo() throws Exception {
        try {
            return listarActivos();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public Videojuego findByIdAndActivo(long id) throws Exception {
        try {
            return findById(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Transactional
    public List<Videojuego> findByTitle(String q) throws Exception {
        try {
            return this.repositorio.findByTitle(q);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }
}
