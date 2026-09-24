package com.zero.ecommerce.entity;

import com.zero.ecommerce.entity.enums.TipoEmpresa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Empresa {

    @Id
    private String id;

    private String razonSocial;
    private String cuit;

    @Enumerated(EnumType.STRING)
    private TipoEmpresa tipoSucursal;

    private boolean eliminado;

    @OneToMany
    private List<Contacto> contactos;

    @OneToOne
    private ConfiguracionCorreoEmpresa configuracionCorreoEmpresa;
}
