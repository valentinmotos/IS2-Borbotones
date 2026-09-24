package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.TipoDocumento;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Persona {

    @Id
    private String id;

    private String nombre;
    private String apellido;

    @Temporal(TemporalType.DATE)
    private java.util.Date fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    private String numeroDocumento;
    private boolean eliminado;

    @OneToOne
    private Imagen imagen;

    @OneToOne
    private Usuario usuario;
}
