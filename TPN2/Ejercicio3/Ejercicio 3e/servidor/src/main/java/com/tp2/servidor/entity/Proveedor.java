package com.tp2.servidor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Proveedor {
    @Id
    private String id = UUID.randomUUID().toString();
    private String nombre;
    private boolean eliminado;
}
