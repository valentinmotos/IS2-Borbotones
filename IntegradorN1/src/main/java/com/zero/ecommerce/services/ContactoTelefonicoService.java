package com.zero.ecommerce.services;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ContactoTelefonicoRepository;

@Service
@Transactional(readOnly = true)
public class ContactoTelefonicoService {

    private static final int LARGO_MAXIMO_OBSERVACION = 255;
    // Dígitos con espacios, guiones, paréntesis y un + inicial opcional: "+54 261 423-1234".
    private static final Pattern TELEFONO = Pattern.compile("\\+?[\\d ()-]{6,25}");

    private final ContactoTelefonicoRepository repository;

    public ContactoTelefonicoService(ContactoTelefonicoRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public ContactoTelefonico crearContactoTelefonico(String telefono, TipoTelefono tipoTelefono,
            TipoContacto tipoContacto, String observacion) throws ErrorServiceException {
        validar(telefono, tipoTelefono, tipoContacto, observacion);
        ContactoTelefonico contacto = new ContactoTelefonico();
        asignarTelefono(contacto, telefono, tipoTelefono, tipoContacto, observacion);
        return repository.save(contacto);
    }

    public void validar(String telefono, TipoTelefono tipoTelefono, TipoContacto tipoContacto,
            String observacion) throws ErrorServiceException {
        if (telefono == null || telefono.isBlank()) {
            throw new ErrorServiceException("El teléfono es obligatorio.");
        }
        if (!TELEFONO.matcher(telefono.strip()).matches()) {
            throw new ErrorServiceException(
                    "El teléfono solo puede tener números, espacios, guiones, paréntesis y un + inicial.");
        }
        if (tipoTelefono == null) {
            throw new ErrorServiceException("El tipo de teléfono es obligatorio.");
        }
        if (tipoTelefono == TipoTelefono.CELULAR) {
            String soloDigitos = normalizarCelular(telefono);
            if (soloDigitos.length() < 8 || soloDigitos.length() > 20) {
                throw new ErrorServiceException(
                        "El teléfono celular debe tener entre 8 y 20 dígitos en formato internacional.");
            }
        }
        if (tipoContacto == null) {
            throw new ErrorServiceException("El tipo de contacto es obligatorio.");
        }
        if (observacion != null && observacion.strip().length() > LARGO_MAXIMO_OBSERVACION) {
            throw new ErrorServiceException(
                    "La observación del contacto no puede superar los " + LARGO_MAXIMO_OBSERVACION + " caracteres.");
        }
    }

    public void validarContactoTelefonico(String telefono, TipoTelefono tipoTelefono, TipoContacto tipoContacto,
            String observacion) throws ErrorServiceException {
        validar(telefono, tipoTelefono, tipoContacto, observacion);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarContactoTelefonico(String id, String telefono, TipoTelefono tipoTelefono,
            TipoContacto tipoContacto, String observacion) throws ErrorServiceException {
        ContactoTelefonico contacto = buscarContactoTelefonico(id);
        validar(telefono, tipoTelefono, tipoContacto, observacion);
        asignarTelefono(contacto, telefono, tipoTelefono, tipoContacto, observacion);
        repository.save(contacto);
    }

    public ContactoTelefonico buscarContactoTelefonico(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El contacto no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El contacto no existe o fue eliminado."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarContactoTelefonico(String id) throws ErrorServiceException {
        ContactoTelefonico contacto = buscarContactoTelefonico(id);
        contacto.setEliminado(true);
        repository.save(contacto);
    }

    public List<ContactoTelefonico> listarContactoTelefonico() {
        return repository.findAllByOrderByTelefonoAsc();
    }

    public List<ContactoTelefonico> listarContactoTelefonicoActivo() {
        return repository.findByEliminadoFalseOrderByTelefonoAsc();
    }

    public String normalizarCelular(String telefono) {
        if (telefono == null) {
            return "";
        }
        return telefono.replaceAll("[^0-9]", "");
    }

    private void asignarTelefono(ContactoTelefonico contacto, String telefono, TipoTelefono tipoTelefono,
            TipoContacto tipoContacto, String observacion) {
        if (tipoTelefono == TipoTelefono.CELULAR) {
            contacto.setTelefono(normalizarCelular(telefono));
        } else {
            contacto.setTelefono(telefono.strip());
        }
        contacto.setTipoTelefono(tipoTelefono);
        contacto.setTipoContacto(tipoContacto);
        contacto.setObservacion(opcional(observacion));
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
