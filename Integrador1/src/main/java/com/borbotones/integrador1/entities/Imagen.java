package com.borbotones.integrador1.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Imagen {

    @Id
    private String id;

    private String nombre;
    private String mime;

    @Lob
    private byte[] contenido;

    @Enumerated(EnumType.STRING)
    private TipoImagen tipoImagen;

    private boolean eliminado;
}