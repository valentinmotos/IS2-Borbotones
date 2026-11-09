package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.dulcemayor.dto.FormaDePagoRequest;
import com.dulcemayor.dto.FormaDePagoResponse;
import com.dulcemayor.entity.FormaDePago;

@Mapper(componentModel = "spring")
public interface FormaDePagoMapper extends CrudMapper<FormaDePago, FormaDePagoRequest, FormaDePagoResponse> {
    @Override
    @Mapping(target = "activo", expression = "java(!entidad.isEliminado())")
    FormaDePagoResponse aRespuesta(FormaDePago entidad);
}
