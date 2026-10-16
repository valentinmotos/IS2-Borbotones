package com.example.servidor.mapper;

import com.example.servidor.dto.AutorDTO;
import com.example.servidor.model.Autor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AutorMapper {
    AutorDTO toDto(Autor autor);
    Autor toEntity(AutorDTO dto);
}
