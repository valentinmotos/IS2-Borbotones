package com.example.servidor.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Localidad extends EntidadBase {
    private String denominacion;
}
