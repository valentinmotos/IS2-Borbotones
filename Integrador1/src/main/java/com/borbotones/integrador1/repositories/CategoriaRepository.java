package com.borbotones.integrador1.repositories;

import com.borbotones.integrador1.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Categoria.
 * Proporciona métodos de consulta para búsqueda por id, nombre,
 * listado completo y listado de registros activos (eliminado = false).
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, String> {

    /**
     * Busca una categoría por su nombre exacto.
     */
    Optional<Categoria> findByNombre(String nombre);

    /**
     * Busca una categoría por su nombre ignorando mayúsculas y minúsculas.
     */
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe una categoría con el nombre indicado (case-insensitive).
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe otra categoría con el mismo nombre excluyendo un ID específico (para edición).
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, String id);

    /**
     * Retorna todas las categorías activas (eliminado = false).
     */
    List<Categoria> findByEliminadoFalse();

    /**
     * Busca una categoría activa por su ID.
     */
    Optional<Categoria> findByIdAndEliminadoFalse(String id);

    /**
     * Busca una categoría activa por su nombre ignorando mayúsculas y minúsculas.
     */
    Optional<Categoria> findByNombreIgnoreCaseAndEliminadoFalse(String nombre);

    /**
     * Busca categorías por coincidencia parcial en el nombre.
     */
    List<Categoria> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Busca categorías activas por coincidencia parcial en el nombre.
     */
    List<Categoria> findByNombreContainingIgnoreCaseAndEliminadoFalse(String nombre);
}
