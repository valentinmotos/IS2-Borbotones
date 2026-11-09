package com.dulcemayor.dto.mapper;

import java.util.List;

import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.dulcemayor.entity.BaseEntity;

/**
 * Mapper genérico que usa AbstractCrudService (patrón DTO, E0-08). Cada ABM declara una
 * interfaz que lo extiende fijando sus tipos y MapStruct genera la implementación:
 * {@code @Mapper(componentModel = "spring") interface CategoriaMapper
 * extends CrudMapper<Categoria, CategoriaRequest, CategoriaResponse> {}}
 *
 * @param <E> entidad
 * @param <Q> DTO de request (alta y modificación)
 * @param <S> DTO de respuesta
 */
public interface CrudMapper<E extends BaseEntity, Q, S> {

    /** Entidad → DTO de respuesta. */
    S aRespuesta(E entidad);

    List<S> aRespuestas(List<E> entidades);

    /** Copia los datos del request sobre la entidad. El id y la baja lógica nunca vienen del cliente. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    void actualizar(Q request, @MappingTarget E entidad);
}
