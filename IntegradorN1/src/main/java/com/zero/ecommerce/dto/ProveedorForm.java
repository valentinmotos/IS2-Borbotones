package com.zero.ecommerce.dto;

import java.util.ArrayList;
import java.util.List;

import com.zero.ecommerce.entities.Contacto;
import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.TipoTelefono;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProveedorForm {

    private String razonSocial;
    private List<ContactoItemDTO> contactos = new ArrayList<>();

    /** Formulario de alta: arranca con una fila de correo y una de celular, que son las obligatorias. */
    public static ProveedorForm nuevo() {
        ProveedorForm form = new ProveedorForm();
        form.getContactos().add(new ContactoItemDTO(null, ContactoItemDTO.CORREO, null, "EMPRESA", null));
        form.getContactos().add(new ContactoItemDTO(null, ContactoItemDTO.CELULAR, null, "EMPRESA", null));
        return form;
    }

    /** Formulario de edición con los contactos activos del proveedor. */
    public static ProveedorForm desde(Proveedor proveedor) {
        ProveedorForm form = new ProveedorForm();
        form.setRazonSocial(proveedor.getRazonSocial());
        for (Contacto contacto : proveedor.getContactos()) {
            if (contacto.isEliminado()) {
                continue;
            }
            ContactoItemDTO item = new ContactoItemDTO();
            item.setId(contacto.getId());
            item.setTipoContacto(contacto.getTipoContacto() != null ? contacto.getTipoContacto().name() : null);
            item.setObservacion(contacto.getObservacion());
            if (contacto instanceof ContactoCorreoElectronico correo) {
                item.setTipo(ContactoItemDTO.CORREO);
                item.setValor(correo.getEmail());
            } else if (contacto instanceof ContactoTelefonico telefono) {
                item.setTipo(telefono.getTipoTelefono() == TipoTelefono.CELULAR
                        ? ContactoItemDTO.CELULAR : ContactoItemDTO.FIJO);
                item.setValor(telefono.getTelefono());
            }
            form.getContactos().add(item);
        }
        return form;
    }
}
