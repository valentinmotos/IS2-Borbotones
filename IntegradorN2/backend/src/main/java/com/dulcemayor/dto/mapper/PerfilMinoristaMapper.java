package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.dulcemayor.dto.PerfilMinoristaResponse;
import com.dulcemayor.entity.Minorista;

/** E2-07: minorista → perfil con la dirección de entrega aplanada. */
@Mapper(componentModel = "spring")
public interface PerfilMinoristaMapper {

    @Mapping(target = "correo", source = "usuario.nombreUsuario")
    @Mapping(target = "departamentoId", source = "direccionEntrega.localidad.departamento.id")
    @Mapping(target = "departamento", source = "direccionEntrega.localidad.departamento.nombre")
    @Mapping(target = "localidadId", source = "direccionEntrega.localidad.id")
    @Mapping(target = "localidad", source = "direccionEntrega.localidad.nombre")
    @Mapping(target = "codigoPostal", source = "direccionEntrega.localidad.codigoPostal")
    @Mapping(target = "calle", source = "direccionEntrega.calle")
    @Mapping(target = "numeracion", source = "direccionEntrega.numeracion")
    @Mapping(target = "barrio", source = "direccionEntrega.barrio")
    @Mapping(target = "manzanaPiso", source = "direccionEntrega.manzanaPiso")
    @Mapping(target = "casaDepartamento", source = "direccionEntrega.casaDepartamento")
    @Mapping(target = "referencia", source = "direccionEntrega.referencia")
    @Mapping(target = "latitud", source = "direccionEntrega.latitud")
    @Mapping(target = "longitud", source = "direccionEntrega.longitud")
    PerfilMinoristaResponse aRespuesta(Minorista minorista);
}
