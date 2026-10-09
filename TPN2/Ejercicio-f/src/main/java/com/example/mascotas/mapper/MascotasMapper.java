package com.example.mascotas.mapper;

import com.example.mascotas.dto.Dto.*;
import com.example.mascotas.entidades.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MascotasMapper {
    ZonaDto zona(Zona entity);
    @Mapping(target = "id", ignore = true)
    Zona zona(ZonaRequest request);

    @Mapping(target = "zonaId", source = "zona.id")
    @Mapping(target = "fotoId", source = "foto.id")
    UsuarioDto usuario(Usuario entity);

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "usuarioNombre", source = "usuario.nombre")
    @Mapping(target = "usuarioApellido", source = "usuario.apellido")
    @Mapping(target = "zonaId", source = "usuario.zona.id")
    @Mapping(target = "fotoId", source = "foto.id")
    MascotaDto mascota(Mascota entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "alta", ignore = true)
    @Mapping(target = "baja", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "foto", ignore = true)
    Mascota mascota(MascotaRequest request);

    @Mapping(target = "mascota1Id", source = "mascota1.id")
    @Mapping(target = "mascota2Id", source = "mascota2.id")
    @Mapping(target = "match", expression = "java(entity.getRespuesta() != null)")
    VotoDto voto(Voto entity);
    FotoDto foto(Foto entity);
}
