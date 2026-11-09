package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.dulcemayor.dto.LocalidadRequest;
import com.dulcemayor.dto.LocalidadResponse;
import com.dulcemayor.entity.Localidad;

@Mapper(componentModel = "spring")
public interface LocalidadMapper extends CrudMapper<Localidad, LocalidadRequest, LocalidadResponse> {

    @Override
    @Mapping(source = "departamento.id", target = "departamentoId")
    @Mapping(source = "departamento.nombre", target = "departamentoNombre")
    LocalidadResponse aRespuesta(Localidad entidad);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "departamento", ignore = true)
    void actualizar(LocalidadRequest request, @MappingTarget Localidad entidad);
}
