package com.zero.ecommerce.entities;

import java.util.ArrayList;
import java.util.List;

import com.zero.ecommerce.entities.enums.TipoEmpresa;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Empresa extends BaseEntity {

    private String razonSocial;

    private String cuit;

    @Enumerated(EnumType.STRING)
    private TipoEmpresa tipoSucursal;

    @OneToOne
    private Direccion direccion;

    // La clave foránea queda en la tabla contacto, sin tabla intermedia.
    @OneToMany
    @JoinColumn(name = "empresa_id")
    private List<Contacto> contactos = new ArrayList<>();
}
