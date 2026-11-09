package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.dto.ImagenDTO;
import com.dulcemayor.entity.Imagen;

@Mapper(componentModel = "spring")
public interface ImagenMapper extends CrudMapper<Imagen, ImagenCargaDTO, ImagenDTO> {
    @Override
    @Mapping(target = "url", expression = "java(\"/api/imagenes/\" + imagen.getId())")
    ImagenDTO aRespuesta(Imagen imagen);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "mime", ignore = true)
    @Mapping(target = "contenido", ignore = true)
    @Mapping(target = "tipoImagen", ignore = true)
    void actualizar(ImagenCargaDTO request, @MappingTarget Imagen entidad);
}
