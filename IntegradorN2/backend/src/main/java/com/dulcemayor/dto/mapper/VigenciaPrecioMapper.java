package com.dulcemayor.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.dulcemayor.dto.VigenciaPrecioResponse;
import com.dulcemayor.entity.VigenciaPrecio;

@Mapper(componentModel = "spring")
public interface VigenciaPrecioMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "vigente", expression = "java(vigencia.getFechaHasta() == null)")
    VigenciaPrecioResponse aRespuesta(VigenciaPrecio vigencia);

    List<VigenciaPrecioResponse> aRespuestas(List<VigenciaPrecio> vigencias);
}
