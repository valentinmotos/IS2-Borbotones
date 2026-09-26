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

    private String id;
    private String razonSocial;
    private List<ContactoItemDTO> contactos = new ArrayList<>();

    public static ProveedorForm desde(Proveedor proveedor) {
        ProveedorForm form = new ProveedorForm();
        form.setId(proveedor.getId());
        form.setRazonSocial(proveedor.getRazonSocial());
        for (Contacto c : proveedor.getContactos()) {
            if (!c.isEliminado()) {
                ContactoItemDTO item = new ContactoItemDTO();
                item.setId(c.getId());
                item.setTipoContacto(c.getTipoContacto() != null ? c.getTipoContacto().name() : "EMPRESA");
                item.setObservacion(c.getObservacion());
                if (c instanceof ContactoCorreoElectronico correo) {
                    item.setTipo("CORREO");
                    item.setValor(correo.getEmail());
                } else if (c instanceof ContactoTelefonico tel) {
                    item.setTipo(tel.getTipoTelefono() == TipoTelefono.CELULAR ? "CELULAR" : "FIJO");
                    item.setValor(tel.getTelefono());
                }
                form.getContactos().add(item);
            }
        }
        return form;
    }
}
