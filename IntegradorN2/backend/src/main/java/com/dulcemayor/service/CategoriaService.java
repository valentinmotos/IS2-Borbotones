package com.dulcemayor.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.dto.mapper.CategoriaMapper;
import com.dulcemayor.entity.Categoria;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.CategoriaRepository;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.utils.TextoUtils;

/**
 * ABM de categorías: ABM de referencia de la plantilla (Template Method, participante: clase
 * concreta). Todo ABM nuevo copia esta estructura: redefine los ganchos de AbstractCrudService
 * y expone los métodos con los nombres del UML, que delegan en la plantilla.
 *
 * Reglas (de IntegradorN1): nombre obligatorio, sin duplicados entre las activas sin importar
 * mayúsculas ni tildes, y no se da de baja si tiene productos activos.
 */
@Service
@Transactional(readOnly = true)
public class CategoriaService extends AbstractCrudService<Categoria, CategoriaRequest, CategoriaResponse> {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, CategoriaMapper mapper,
            ProductoRepository productoRepository) {
        super(categoriaRepository, mapper);
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // ---- Métodos del UML (delegan en la plantilla) ----

    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest request) {
        return crear(request);
    }

    @Transactional
    public CategoriaResponse modificarCategoria(String id, CategoriaRequest request) {
        return modificar(id, request);
    }

    @Transactional
    public void eliminarCategoria(String id) {
        eliminar(id);
    }

    public CategoriaResponse buscarCategoria(String id) {
        return buscar(id);
    }

    public List<CategoriaResponse> listarCategoriaActivo() {
        return listarActivos();
    }

    /** validar(nombre) del UML: reglas del alta. */
    public void validar(String nombre) {
        validar(new CategoriaRequest(nombre), null);
    }

    // ---- Ganchos de la plantilla ----

    @Override
    protected void validar(CategoriaRequest request, String idActual) {
        String nombre = request == null ? null : request.nombre();
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre de la categoría es obligatorio.", "nombre");
        }
        boolean duplicada = categoriaRepository.findByEliminadoFalse().stream()
                .filter(categoria -> !categoria.getId().equals(idActual))
                .anyMatch(categoria -> TextoUtils.mismoNombre(categoria.getNombre(), nombre));
        if (duplicada) {
            throw new DuplicadoException("Ya existe una categoría con ese nombre.", "nombre");
        }
    }

    @Override
    protected Categoria nuevaEntidad() {
        return new Categoria();
    }

    @Override
    protected void asignar(Categoria categoria, CategoriaRequest request) {
        super.asignar(categoria, request);
        categoria.setNombre(request.nombre().strip());
    }

    @Override
    protected void antesDeEliminar(Categoria categoria) {
        if (productoRepository.existsByCategoria_IdAndEliminadoFalse(categoria.getId())) {
            throw new ErrorServiceException(
                    "No se puede dar de baja la categoría porque tiene productos activos.");
        }
    }

    @Override
    protected String mensajeNoEncontrado() {
        return "No se encontró la categoría.";
    }

    @Override
    protected Sort orden() {
        return Sort.by("nombre");
    }
}
