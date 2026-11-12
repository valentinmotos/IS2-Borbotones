package com.dulcemayor.service;

import com.dulcemayor.entity.ContactoCorreoElectronico;
import com.dulcemayor.entity.Minorista;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.repository.ContactoCorreoElectronicoRepository;
import com.dulcemayor.utils.TextoUtils;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Correo al que se le escribe al responsable de un comercio: el primer contacto de correo válido del
 * minorista o, si no tiene, el correo de su usuario.
 *
 * <p>NOTA (E5-02): extraído de {@code FacturaCorreoService} (E4-07) para reutilizarlo en el aviso
 * "pedido en camino". El criterio no cambió.</p>
 */
@Component
public class DestinatarioMinorista {

    private final ContactoCorreoElectronicoRepository contactos;

    public DestinatarioMinorista(ContactoCorreoElectronicoRepository contactos) {
        this.contactos = contactos;
    }

    public Optional<String> buscar(Minorista minorista) {
        if (minorista == null) {
            return Optional.empty();
        }
        return contactos.findByMinorista_IdAndEliminadoFalseOrderByIdAsc(minorista.getId()).stream()
                .map(ContactoCorreoElectronico::getEmail)
                .filter(TextoUtils::esCorreoValido)
                .findFirst()
                .or(() -> Optional.ofNullable(minorista.getUsuario())
                        .filter(usuario -> !usuario.isEliminado())
                        .map(Usuario::getNombreUsuario)
                        .filter(TextoUtils::esCorreoValido))
                .map(String::strip);
    }
}
