package com.zero.ecommerce.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;

@Service
@Transactional(readOnly = true)
public class SubCategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SubCategoriaRepository subCategoriaRepository;
    private final ProductoRepository productoRepository;

    public SubCategoriaService(CategoriaRepository categoriaRepository,
            SubCategoriaRepository subCategoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.subCategoriaRepository = subCategoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void crearSubCategoria(String categoriaId, String nombre) throws ErrorServiceException {
        Categoria categoria = validarCategoriaPadre(categoriaId);
        validar(nombre, categoriaId, null);
        SubCategoria subCategoria = new SubCategoria();
        subCategoria.setCategoria(categoria);
        subCategoria.setNombre(normalizar(nombre));
        subCategoriaRepository.save(subCategoria);
    }

    public void validar(String nombre) throws ErrorServiceException {
        validar(nombre, null, null);
    }

    private void validar(String nombre, String categoriaId, String idActual) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("El nombre de la subcategoría es obligatorio.");
        }
        String nombreNormalizado = normalizar(nombre);
        Optional<SubCategoria> duplicada = subCategoriaRepository
                .findByCategoria_IdAndNombreIgnoreCaseAndEliminadoFalse(categoriaId, nombreNormalizado);
        if (duplicada.isPresent() && (idActual == null || !duplicada.get().getId().equals(idActual))) {
            throw new ErrorServiceException(
                    "Ya existe una subcategoría con ese nombre dentro de la categoría seleccionada.");
        }
    }

    private String normalizar(String nombre) {
        return nombre == null ? null : nombre.trim();
    }

    private Categoria validarCategoriaPadre(String categoriaId) throws ErrorServiceException {
        if (categoriaId == null || categoriaId.isBlank()) {
            throw new ErrorServiceException("La categoría seleccionada no existe o no es válida.");
        }
        return categoriaRepository.findByIdAndEliminadoFalse(categoriaId)
                .orElseThrow(() -> new ErrorServiceException("La categoría seleccionada no existe o no es válida."));
    }

    public SubCategoria buscarSubCategoria(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La subcategoría no existe o fue eliminada.");
        }
        return subCategoriaRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La subcategoría no existe o fue eliminada."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarSubCategoria(String id, String categoriaId, String nombre) throws ErrorServiceException {
        SubCategoria subCategoria = buscarSubCategoria(id);
        Categoria categoria = validarCategoriaPadre(categoriaId);
        validar(nombre, categoriaId, id);
        subCategoria.setCategoria(categoria);
        subCategoria.setNombre(normalizar(nombre));
        subCategoriaRepository.save(subCategoria);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarSubCategoria(String id) throws ErrorServiceException {
        SubCategoria subCategoria = buscarSubCategoria(id);
        boolean tieneProductosActivos = productoRepository.existsBySubCategoria_IdAndEliminadoFalse(id);
        if (tieneProductosActivos) {
            throw new ErrorServiceException("No se puede eliminar la subcategoría porque tiene productos activos.");
        }
        subCategoria.setEliminado(true);
        subCategoriaRepository.save(subCategoria);
    }

    public List<SubCategoria> listarSubCategoria() {
        return subCategoriaRepository.findAllByOrderByNombreAsc();
    }

    public List<SubCategoria> listarSubCategoriaActiva() {
        return subCategoriaRepository.findByEliminadoFalseOrderByNombreAsc();
    }

    public List<SubCategoria> listarSubCategoriaPorCategoria(String categoriaId) {
        return subCategoriaRepository.findByCategoria_IdAndEliminadoFalseOrderByNombreAsc(categoriaId);
    }
}
