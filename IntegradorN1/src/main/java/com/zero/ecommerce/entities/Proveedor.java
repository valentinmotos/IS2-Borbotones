package com.zero.ecommerce.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Proveedor extends BaseEntity {

    private String razonSocial;

    // La clave foránea queda en la tabla contacto, sin tabla intermedia.
    @OneToMany
    @JoinColumn(name = "proveedor_id")
    private List<Contacto> contactos = new ArrayList<>();
}
