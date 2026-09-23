package com.borbotones.integrador1.services;

import com.borbotones.integrador1.entities.Categoria;
import com.borbotones.integrador1.repositories.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Servicio de negocio para la gestión de Categoria.
 * 
 * Implementa las operaciones definidas en el Diagrama de Diseño Integrado:
 * - crearCategoria(nombre)
 * - validar(nombre)
 * - buscarCategoria(id)
 * - buscarCategoriaPorNombre(nombre)
 * - modificarCategoria(id, nombre)
 * - eliminarCategoria(id) (baja lógica)
 * - listarCategoria()
 * - listarCategoriaActivo()
 */
@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Valida que el nombre cumpla con las reglas básicas del negocio.
     *
     * @param nombre nombre a validar
     * @throws IllegalArgumentException si el nombre es nulo, vacío o supera el límite permitido
     */
    public void validar(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío.");
        }
        String nombreLimpio = nombre.trim();
        if (nombreLimpio.length() < 2) {
            throw new IllegalArgumentException("El nombre de la categoría debe tener al menos 2 caracteres.");
        }
        if (nombreLimpio.length() > 100) {
            throw new IllegalArgumentException("El nombre de la categoría no puede superar los 100 caracteres.");
        }
    }

    /**
     * Crea y persiste una nueva categoría en el sistema.
     *
     * @param nombre nombre de la nueva categoría
     * @throws IllegalArgumentException si la validación falla o ya existe una categoría con ese nombre
     */
    @Transactional
    public void crearCategoria(String nombre) {
        validar(nombre);
        String nombreLimpio = nombre.trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre '" + nombreLimpio + "'.");
        }

        Categoria categoria = new Categoria(nombreLimpio);
        categoriaRepository.save(categoria);
    }

    /**
     * Busca una categoría por su identificador único.
     *
     * @param id identificador de la categoría
     * @return la categoría encontrada
     * @throws IllegalArgumentException si el ID es nulo o vacío
     * @throws NoSuchElementException si no existe la categoría con ese ID
     */
    public Categoria buscarCategoria(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la categoría es requerido.");
        }
        return categoriaRepository.findById(id.trim())
                .orElseThrow(() -> new NoSuchElementException("No se encontró la categoría con ID: " + id));
    }

    /**
     * Busca una categoría por su nombre (insensible a mayúsculas y minúsculas).
     *
     * @param nombre nombre de la categoría
     * @return la categoría encontrada
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     * @throws NoSuchElementException si no existe la categoría con ese nombre
     */
    public Categoria buscarCategoriaPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es requerido para la búsqueda.");
        }
        return categoriaRepository.findByNombreIgnoreCase(nombre.trim())
                .orElseThrow(() -> new NoSuchElementException("No se encontró la categoría con nombre: " + nombre));
    }

    /**
     * Modifica el nombre de una categoría existente.
     *
     * @param id identificador de la categoría a modificar
     * @param nombre nuevo nombre para la categoría
     * @throws IllegalArgumentException si la validación falla o ya existe otra categoría con ese nombre
     * @throws NoSuchElementException si la categoría no existe
     */
    @Transactional
    public void modificarCategoria(String id, String nombre) {
        validar(nombre);
        Categoria categoria = buscarCategoria(id);
        String nombreLimpio = nombre.trim();

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombreLimpio, categoria.getId())) {
            throw new IllegalArgumentException("Ya existe otra categoría con el nombre '" + nombreLimpio + "'.");
        }

        categoria.setNombre(nombreLimpio);
        categoriaRepository.save(categoria);
    }

    /**
     * Realiza la baja lógica de la categoría, marcando su atributo eliminado como true.
     * No realiza eliminación física en la base de datos.
     *
     * @param id identificador de la categoría a eliminar
     * @throws NoSuchElementException si la categoría no existe
     */
    @Transactional
    public void eliminarCategoria(String id) {
        Categoria categoria = buscarCategoria(id);
        categoria.setEliminado(true);
        categoriaRepository.save(categoria);
    }

    /**
     * Reactiva una categoría que había sido dada de baja lógica.
     *
     * @param id identificador de la categoría a reactivar
     */
    @Transactional
    public void reactivarCategoria(String id) {
        Categoria categoria = buscarCategoria(id);
        categoria.setEliminado(false);
        categoriaRepository.save(categoria);
    }

    /**
     * Lista todas las categorías registradas en el sistema (tanto activas como eliminadas).
     *
     * @return colección con todas las categorías
     */
    public Collection<Categoria> listarCategoria() {
        return categoriaRepository.findAll();
    }

    /**
     * Lista únicamente las categorías activas (eliminado = false).
     *
     * @return colección de categorías activas
     */
    public Collection<Categoria> listarCategoriaActivo() {
        return categoriaRepository.findByEliminadoFalse();
    }

    /**
     * Búsqueda por filtro de texto para el listado administrativo.
     *
     * @param criterio término de búsqueda en el nombre
     * @param incluirEliminadas si se deben incluir las categorías dadas de baja
     * @return lista de categorías que coinciden con el criterio
     */
    public List<Categoria> buscarPorCriterio(String criterio, boolean incluirEliminadas) {
        if (criterio == null || criterio.trim().isEmpty()) {
            return incluirEliminadas ? categoriaRepository.findAll() : categoriaRepository.findByEliminadoFalse();
        }
        String filtro = criterio.trim();
        return incluirEliminadas
                ? categoriaRepository.findByNombreContainingIgnoreCase(filtro)
                : categoriaRepository.findByNombreContainingIgnoreCaseAndEliminadoFalse(filtro);
    }
}
