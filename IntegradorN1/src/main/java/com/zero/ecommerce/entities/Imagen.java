package com.zero.ecommerce.entities;

import com.zero.ecommerce.entities.enums.TipoImagen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Imagen extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String mime;

    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoImagen tipoImagen;
}
