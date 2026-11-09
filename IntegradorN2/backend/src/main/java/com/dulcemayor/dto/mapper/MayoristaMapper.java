package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import com.dulcemayor.dto.MayoristaRequest;
import com.dulcemayor.dto.MayoristaResponse;
import com.dulcemayor.entity.Mayorista;

@Mapper(componentModel = "spring")
public interface MayoristaMapper extends CrudMapper<Mayorista, MayoristaRequest, MayoristaResponse> {
}
