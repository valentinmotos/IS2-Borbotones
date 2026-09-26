package com.zero.ecommerce.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.zero.ecommerce.entities.enums.TipoTelefono;

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

    /** Experto: el proveedor sabe cuáles de sus correos siguen activos. */
    public List<ContactoCorreoElectronico> listarCorreoActivo() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoCorreoElectronico)
                .map(ContactoCorreoElectronico.class::cast)
                .toList();
    }

    /** Experto: el proveedor sabe cuáles de sus teléfonos (fijos y celulares) siguen activos. */
    public List<ContactoTelefonico> listarTelefonoActivo() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoTelefonico)
                .map(ContactoTelefonico.class::cast)
                .toList();
    }

    /** Experto: el celular al que se le escribe por WhatsApp (el primero activo). Lo usan E3-03 y E5-04. */
    public Optional<ContactoTelefonico> buscarCelularActivo() {
        return listarTelefonoActivo().stream()
                .filter(t -> t.getTipoTelefono() == TipoTelefono.CELULAR)
                .findFirst();
    }
}
