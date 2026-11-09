package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import com.dulcemayor.dto.DepartamentoRequest;
import com.dulcemayor.dto.DepartamentoResponse;
import com.dulcemayor.entity.Departamento;

@Mapper(componentModel = "spring")
public interface DepartamentoMapper extends CrudMapper<Departamento, DepartamentoRequest, DepartamentoResponse> {
}
