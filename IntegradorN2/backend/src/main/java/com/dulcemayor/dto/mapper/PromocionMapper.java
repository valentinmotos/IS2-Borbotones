package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.dulcemayor.dto.PromocionRequest;
import com.dulcemayor.dto.PromocionResponse;
import com.dulcemayor.entity.Promocion;

@Mapper(componentModel = "spring")
public interface PromocionMapper extends CrudMapper<Promocion, PromocionRequest, PromocionResponse> {

    @Override
    @Mapping(target = "idProducto", source = "producto.id")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    PromocionResponse aRespuesta(Promocion promocion);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "producto", ignore = true)
    void actualizar(PromocionRequest request, @MappingTarget Promocion promocion);
}
