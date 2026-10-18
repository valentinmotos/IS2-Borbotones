package com.zero.ecommerce.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /** Experto: la empresa sabe cuál es su correo de contacto activo. */
    public Optional<ContactoCorreoElectronico> buscarCorreoActivo() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoCorreoElectronico)
                .map(ContactoCorreoElectronico.class::cast)
                .findFirst();
    }

    /** Experto: la empresa sabe cuál es su teléfono de contacto activo. */
    public Optional<ContactoTelefonico> buscarTelefonoActivo() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoTelefonico)
                .map(ContactoTelefonico.class::cast)
                .findFirst();
    }
}
