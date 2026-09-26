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

    /** Devuelve la lista de correos de contacto activos. */
    public List<ContactoCorreoElectronico> getCorreosActivos() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoCorreoElectronico)
                .map(ContactoCorreoElectronico.class::cast)
                .toList();
    }

    /** Devuelve la lista de teléfonos de contacto activos. */
    public List<ContactoTelefonico> getTelefonosActivos() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoTelefonico)
                .map(ContactoTelefonico.class::cast)
                .toList();
    }

    /** Experto: devuelve el primer teléfono celular activo para WhatsApp. */
    public Optional<ContactoTelefonico> buscarPrimerCelularActivo() {
        return contactos.stream()
                .filter(c -> !c.isEliminado() && c instanceof ContactoTelefonico)
                .map(ContactoTelefonico.class::cast)
                .filter(t -> t.getTipoTelefono() == TipoTelefono.CELULAR)
                .findFirst();
    }

    /** Devuelve el número de celular del primer celular activo, o null si no tiene. */
    public String getPrimerCelular() {
        return buscarPrimerCelularActivo()
                .map(ContactoTelefonico::getTelefono)
                .orElse(null);
    }
}
