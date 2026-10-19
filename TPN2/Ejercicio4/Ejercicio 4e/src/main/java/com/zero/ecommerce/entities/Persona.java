package com.zero.ecommerce.entities;

import java.time.LocalDate;

import com.zero.ecommerce.entities.enums.TipoDocumento;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Persona extends BaseEntity {

    private String nombre;

    private String apellido;

    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    private String numeroDocumento;

    @OneToOne
    private Usuario usuario;

    @OneToOne
    private Imagen imagen;

    @OneToOne
    private Direccion direccion;

    @OneToOne
    private ContactoTelefonico telefono;
}
