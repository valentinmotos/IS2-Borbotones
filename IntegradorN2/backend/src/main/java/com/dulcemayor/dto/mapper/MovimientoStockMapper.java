package com.dulcemayor.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.dulcemayor.dto.MovimientoStockResponse;
import com.dulcemayor.entity.MovimientoStock;

@Mapper(componentModel = "spring")
public interface MovimientoStockMapper {

    MovimientoStockResponse aRespuesta(MovimientoStock movimiento);

    List<MovimientoStockResponse> aRespuestas(List<MovimientoStock> movimientos);
}
