package com.zero.ecommerce.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.CategoriaArbolDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SubCategoriaRepository subCategoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
            SubCategoriaRepository subCategoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.subCategoriaRepository = subCategoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void crearCategoria(String nombre) throws ErrorServiceException {
        validar(nombre, null);
        Categoria categoria = new Categoria();
        categoria.setNombre(normalizar(nombre));
        categoriaRepository.save(categoria);
    }

    public void validar(String nombre) throws ErrorServiceException {
        validar(nombre, null);
    }

    private void validar(String nombre, String idActual) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre de la categoría es obligatorio.");
        }

        String nombreNormalizado = normalizar(nombre);
        Optional<Categoria> duplicada = categoriaRepository
                .findByNombreIgnoreCaseAndEliminadoFalse(nombreNormalizado);
        if (duplicada.isPresent() && (idActual == null || !duplicada.get().getId().equals(idActual))) {
            throw new ErrorServiceException("Ya existe una categoría con ese nombre.");
        }
    }

    private String normalizar(String nombre) {
        return nombre == null ? null : nombre.trim();
    }

    public Categoria buscarCategoria(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La categoría no existe o fue eliminada.");
        }
        return categoriaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La categoría no existe o fue eliminada."));
    }

    public Categoria buscarCategoriaPorNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("La categoría no existe o fue eliminada.");
        }
        return categoriaRepository.findByNombreIgnoreCaseAndEliminadoFalse(normalizar(nombre))
                .orElseThrow(() -> new ErrorServiceException("La categoría no existe o fue eliminada."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarCategoria(String id, String nombre) throws ErrorServiceException {
        Categoria categoria = buscarCategoria(id);
        validar(nombre, id);
        categoria.setNombre(normalizar(nombre));
        categoriaRepository.save(categoria);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarCategoria(String id) throws ErrorServiceException {
        Categoria categoria = buscarCategoria(id);
        boolean tieneProductosActivos = productoRepository.existsBySubCategoria_Categoria_IdAndEliminadoFalse(id);
        if (tieneProductosActivos) {
            throw new ErrorServiceException("No se puede eliminar la categoría porque tiene productos activos.");
        }
        categoria.setEliminado(true);
        categoriaRepository.save(categoria);
    }

    public List<Categoria> listarCategoria() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    public List<Categoria> listarCategoriaActiva() {
        return categoriaRepository.findByEliminadoFalseOrderByNombreAsc();
    }

    public List<CategoriaArbolDTO> listarArbolActivo() {
        return listarCategoriaActiva().stream()
                .map(categoria -> new CategoriaArbolDTO(
                        categoria.getId(),
                        categoria.getNombre(),
                        subCategoriaRepository.findByCategoria_IdAndEliminadoFalseOrderByNombreAsc(categoria.getId())))
                .toList();
    }
}
