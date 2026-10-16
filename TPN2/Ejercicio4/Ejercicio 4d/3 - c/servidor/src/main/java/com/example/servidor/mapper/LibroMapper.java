package com.example.servidor.mapper;

import com.example.servidor.dto.LibroDTO;
import com.example.servidor.model.Libro;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LibroMapper {
    LibroDTO toDto(Libro libro);
    Libro toEntity(LibroDTO dto);
}
