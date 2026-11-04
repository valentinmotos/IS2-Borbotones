package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contactos_correo_electronico")
@Getter
@Setter
public class ContactoCorreoElectronico extends BaseEntity {

    @Column(nullable = false, length = 254)
    private String email;

    // Minorista se incorpora en E0-06; se mantiene su UUID según el contrato del UML.
    @Column(name = "minorista_id", nullable = false, length = 36)
    private String idMinorista;
}
