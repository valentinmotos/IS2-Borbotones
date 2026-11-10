package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.dulcemayor.dto.ProductoRequest;
import com.dulcemayor.dto.ProductoResponse;
import com.dulcemayor.entity.Producto;

@Mapper(componentModel = "spring")
public interface ProductoMapper extends CrudMapper<Producto, ProductoRequest, ProductoResponse> {

    @Override
    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNombre", source = "categoria.nombre")
    @Mapping(target = "imagenId", source = "imagen.id")
    ProductoResponse aRespuesta(Producto producto);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "imagen", ignore = true)
    @Mapping(target = "enOferta", ignore = true)
    void actualizar(ProductoRequest request, @MappingTarget Producto producto);
}
