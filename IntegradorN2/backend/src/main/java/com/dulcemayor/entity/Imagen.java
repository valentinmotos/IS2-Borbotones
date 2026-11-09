package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.TipoImagen;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "imagenes")
@Getter
@Setter
public class Imagen extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String mime;

    // byte[] se guarda como BYTEA en PostgreSQL y como binario en H2, sin OID/LOB.
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_imagen", nullable = false, length = 30)
    private TipoImagen tipoImagen;
}
