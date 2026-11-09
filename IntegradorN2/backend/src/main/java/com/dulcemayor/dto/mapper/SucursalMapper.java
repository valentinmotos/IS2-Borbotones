package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.dulcemayor.dto.SucursalRequest;
import com.dulcemayor.dto.SucursalResponse;
import com.dulcemayor.entity.Sucursal;

@Mapper(componentModel = "spring")
public interface SucursalMapper extends CrudMapper<Sucursal, SucursalRequest, SucursalResponse> {
    @Override
    @Mapping(target = "idMayorista", source = "mayorista.id")
    @Mapping(target = "razonSocial", source = "mayorista.razonSocial")
    @Mapping(target = "idLocalidad", source = "direccion.localidad.id")
    @Mapping(target = "localidad", source = "direccion.localidad.nombre")
    @Mapping(target = "codigoPostal", source = "direccion.localidad.codigoPostal")
    @Mapping(target = "calle", source = "direccion.calle")
    @Mapping(target = "numeracion", source = "direccion.numeracion")
    @Mapping(target = "barrio", source = "direccion.barrio")
    @Mapping(target = "manzanaPiso", source = "direccion.manzanaPiso")
    @Mapping(target = "casaDepartamento", source = "direccion.casaDepartamento")
    @Mapping(target = "referencia", source = "direccion.referencia")
    @Mapping(target = "latitud", source = "direccion.latitud")
    @Mapping(target = "longitud", source = "direccion.longitud")
    @Mapping(target = "idImagen", source = "imagen.id")
    @Mapping(target = "urlImagen", expression = "java(sucursal.getImagen() == null ? null : \"/api/imagenes/\" + sucursal.getImagen().getId())")
    SucursalResponse aRespuesta(Sucursal sucursal);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "mayorista", ignore = true)
    @Mapping(target = "direccion", ignore = true)
    @Mapping(target = "imagen", ignore = true)
    void actualizar(SucursalRequest request, @MappingTarget Sucursal entidad);
}
