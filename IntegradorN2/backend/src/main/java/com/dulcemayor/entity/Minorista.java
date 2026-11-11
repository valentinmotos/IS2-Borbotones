package com.dulcemayor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "minoristas")
@Getter
@Setter
public class Minorista extends Persona {

    /**
     * E2-07: dirección de entrega que el minorista carga en su perfil. Desvío del UML (el diagrama
     * no le da dirección al minorista): es opcional porque al registrarse todavía no la tiene.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direccion_entrega_id", unique = true)
    private Direccion direccionEntrega;
}
