package com.dulcemayor.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.dulcemayor.dto.UsuarioInternoRequest;
import com.dulcemayor.dto.UsuarioInternoResponse;
import com.dulcemayor.entity.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioInternoMapper extends CrudMapper<Usuario, UsuarioInternoRequest, UsuarioInternoResponse> {

    @Override
    @Mapping(target = "correo", source = "nombreUsuario")
    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "apellido", ignore = true)
    @Mapping(target = "fechaNacimiento", ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @Mapping(target = "numeroDocumento", ignore = true)
    @Mapping(target = "legajo", ignore = true)
    UsuarioInternoResponse aRespuesta(Usuario usuario);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eliminado", ignore = true)
    @Mapping(target = "nombreUsuario", source = "correo")
    @Mapping(target = "clave", ignore = true)
    @Mapping(target = "claveTemporal", ignore = true)
    @Mapping(target = "proveedorAuth", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "versionToken", ignore = true)
    @Mapping(target = "codigoActivacionHash", ignore = true)
    @Mapping(target = "codigoActivacionExpira", ignore = true)
    @Mapping(target = "codigoActivacionEnviado", ignore = true)
    @Mapping(target = "intentosCodigoActivacion", ignore = true)
    void actualizar(UsuarioInternoRequest request, @MappingTarget Usuario usuario);
}
