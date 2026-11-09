package com.dulcemayor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.dulcemayor.entity.BaseEntity;

/**
 * Consultas comunes con baja lógica que usa AbstractCrudService (E0-08).
 * Spring Data resuelve los métodos derivados para cada entidad concreta; esta interfaz no
 * genera un bean propio (@NoRepositoryBean).
 *
 * Todo repositorio de un ABM que use la plantilla extiende esta interfaz:
 * {@code public interface DepartamentoRepository extends BaseRepository<Departamento> {}}
 */
@NoRepositoryBean
public interface BaseRepository<E extends BaseEntity> extends JpaRepository<E, String> {

    /** Registro activo (no dado de baja) por id. */
    Optional<E> findByIdAndEliminadoFalse(String id);

    /** Registros activos en el orden indicado. */
    List<E> findByEliminadoFalse(Sort orden);
}
