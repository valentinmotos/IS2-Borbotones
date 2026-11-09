package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;

import com.dulcemayor.dto.CategoriaRequest;
import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.entity.Categoria;

/** MapStruct genera CategoriaMapperImpl al compilar. */
@Mapper(componentModel = "spring")
public interface CategoriaMapper extends CrudMapper<Categoria, CategoriaRequest, CategoriaResponse> {
}
