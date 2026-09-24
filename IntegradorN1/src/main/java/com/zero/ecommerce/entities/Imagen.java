package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.TipoImagen;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Imagen extends BaseEntity {

    private String nombre;

    private String mime;

    @Lob
    private byte[] contenido;

    @Enumerated(EnumType.STRING)
    private TipoImagen tipoImagen;
}
