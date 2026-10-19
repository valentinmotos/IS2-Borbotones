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
public class Usuario {
    @Id
    private String id = UUID.randomUUID().toString();
    private String nombre;
    @JsonIgnore
    private String clave;
    private boolean eliminado;
}
