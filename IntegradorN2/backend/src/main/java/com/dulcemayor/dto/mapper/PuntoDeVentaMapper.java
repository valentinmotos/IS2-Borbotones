package com.dulcemayor.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.dulcemayor.dto.PuntoDeVentaResponse;
import com.dulcemayor.entity.PuntoDeVenta;

/** E2-06: punto de venta → respuesta con la dirección aplanada. */
@Mapper(componentModel = "spring")
public interface PuntoDeVentaMapper {
    @Mapping(target = "minoristaId", source = "minorista.id")
    @Mapping(target = "nombreMinorista", expression = "java(punto.getMinorista().getNombre() + \" \" + punto.getMinorista().getApellido())")
    @Mapping(target = "preventorId", source = "preventor.id")
    @Mapping(target = "idLocalidad", source = "direccion.localidad.id")
    @Mapping(target = "localidad", source = "direccion.localidad.nombre")
    @Mapping(target = "idDepartamento", source = "direccion.localidad.departamento.id")
    @Mapping(target = "departamento", source = "direccion.localidad.departamento.nombre")
    @Mapping(target = "codigoPostal", source = "direccion.localidad.codigoPostal")
    @Mapping(target = "calle", source = "direccion.calle")
    @Mapping(target = "numeracion", source = "direccion.numeracion")
    @Mapping(target = "barrio", source = "direccion.barrio")
    @Mapping(target = "manzanaPiso", source = "direccion.manzanaPiso")
    @Mapping(target = "casaDepartamento", source = "direccion.casaDepartamento")
    @Mapping(target = "referencia", source = "direccion.referencia")
    @Mapping(target = "latitud", source = "direccion.latitud")
    @Mapping(target = "longitud", source = "direccion.longitud")
    PuntoDeVentaResponse aRespuesta(PuntoDeVenta punto);

    List<PuntoDeVentaResponse> aRespuestas(List<PuntoDeVenta> puntos);
}
