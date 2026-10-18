package com.tp2.servidor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Imagen {
    @Id
    private String id = UUID.randomUUID().toString();
    private String nombre;
    private String mime;
    @JsonIgnore
    private byte[] contenido;
    private boolean eliminado;
}
