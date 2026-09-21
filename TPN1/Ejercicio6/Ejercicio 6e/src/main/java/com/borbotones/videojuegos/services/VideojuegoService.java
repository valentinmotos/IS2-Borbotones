package com.borbotones.videojuegos.services;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.entities.Videojuego;
import com.borbotones.videojuegos.repositories.CategoriaRepository;
import com.borbotones.videojuegos.repositories.EstudioRepository;
import com.borbotones.videojuegos.repositories.VideojuegoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VideojuegoService implements BaseService<Videojuego> {
    private final VideojuegoRepository repositorio;
    private final EstudioRepository estudioRepository;
    private final CategoriaRepository categoriaRepository;

    public VideojuegoService(
            VideojuegoRepository repositorio,
            EstudioRepository estudioRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.repositorio = repositorio;
        this.estudioRepository = estudioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Videojuego> findAll() {
        return repositorio.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Videojuego findById(long id) {
        validarId(id);
        return repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el videojuego solicitado"));
    }

    @Override
    @Transactional
    public Videojuego saveOne(Videojuego entity) {
        validar(entity);
        entity.setId(0);
        entity.setActivo(true);
        normalizar(entity);
        asignarRelacionesAdministradas(entity);
        return repositorio.save(entity);
    }

    @Override
    @Transactional
    public Videojuego updateOne(Videojuego recibido, long id) {
        validar(recibido);
        Videojuego existente = findById(id);
        normalizar(recibido);
        asignarRelacionesAdministradas(recibido);

        existente.setTitulo(recibido.getTitulo());
        existente.setDescripcion(recibido.getDescripcion());
        existente.setPrecio(recibido.getPrecio());
        existente.setStock(recibido.getStock());
        existente.setFechaLanzamiento(recibido.getFechaLanzamiento());
        existente.setCategoria(recibido.getCategoria());
        existente.setEstudio(recibido.getEstudio());
        if (recibido.getImagen() != null && !recibido.getImagen().isBlank()) {
            existente.setImagen(recibido.getImagen());
        }
        return repositorio.save(existente);
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        Videojuego videojuego = findById(id);
        videojuego.setActivo(false);
        repositorio.save(videojuego);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Videojuego> findAllByActivo() {
        return repositorio.findAllByActivoTrueOrderByTituloAsc();
    }

    @Transactional(readOnly = true)
    public Videojuego findByIdAndActivo(long id) {
        validarId(id);
        return repositorio.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el videojuego solicitado"));
    }

    @Transactional(readOnly = true)
    public List<Videojuego> findByTitle(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return findAllByActivo();
        }
        String normalizada = consulta.trim();
        if (normalizada.length() > 100) {
            throw new IllegalArgumentException("La busqueda no puede superar los 100 caracteres");
        }
        return repositorio.findByActivoTrueAndTituloContainingIgnoreCaseOrderByTituloAsc(normalizada);
    }

    private void asignarRelacionesAdministradas(Videojuego videojuego) {
        long estudioId = videojuego.getEstudio().getId();
        long categoriaId = videojuego.getCategoria().getId();
        Estudio estudio = estudioRepository.findById(estudioId)
                .filter(Estudio::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("El estudio seleccionado no es valido"));
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .filter(Categoria::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("La categoria seleccionada no es valida"));
        videojuego.setEstudio(estudio);
        videojuego.setCategoria(categoria);
    }

    private void normalizar(Videojuego videojuego) {
        videojuego.setTitulo(videojuego.getTitulo().trim());
        videojuego.setDescripcion(videojuego.getDescripcion().trim());
    }

    private void validar(Videojuego videojuego) {
        if (videojuego == null || videojuego.getTitulo() == null || videojuego.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El titulo es obligatorio");
        }
        if (videojuego.getDescripcion() == null) {
            throw new IllegalArgumentException("La descripcion es obligatoria");
        }
        if (videojuego.getEstudio() == null || videojuego.getCategoria() == null) {
            throw new IllegalArgumentException("El estudio y la categoria son obligatorios");
        }
    }

    private void validarId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador de videojuego no es valido");
        }
    }
}
