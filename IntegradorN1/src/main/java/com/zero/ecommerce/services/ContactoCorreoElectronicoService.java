package com.zero.ecommerce.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ContactoCorreoElectronicoRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class ContactoCorreoElectronicoService {

    private static final int LARGO_MAXIMO_CORREO = 150;
    private static final int LARGO_MAXIMO_OBSERVACION = 255;

    private final ContactoCorreoElectronicoRepository repository;

    public ContactoCorreoElectronicoService(ContactoCorreoElectronicoRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public ContactoCorreoElectronico crearContactoCorreoElectronico(String email, TipoContacto tipoContacto,
            String observacion) throws ErrorServiceException {
        validar(email, tipoContacto, observacion);
        ContactoCorreoElectronico contacto = new ContactoCorreoElectronico();
        asignarCorreo(contacto, email, tipoContacto, observacion);
        return repository.save(contacto);
    }

    public void validar(String email, TipoContacto tipoContacto, String observacion) throws ErrorServiceException {
        if (email == null || email.isBlank()) {
            throw new ErrorServiceException("El correo electrónico es obligatorio.");
        }
        if (email.strip().length() > LARGO_MAXIMO_CORREO || !TextoUtils.esCorreoValido(email)) {
            throw new ErrorServiceException("El correo electrónico no tiene un formato válido.");
        }
        if (tipoContacto == null) {
            throw new ErrorServiceException("El tipo de contacto es obligatorio.");
        }
        if (observacion != null && observacion.strip().length() > LARGO_MAXIMO_OBSERVACION) {
            throw new ErrorServiceException(
                    "La observación del contacto no puede superar los " + LARGO_MAXIMO_OBSERVACION + " caracteres.");
        }
    }

    public void validarContactoCorreoElectronico(String email, TipoContacto tipoContacto, String observacion)
            throws ErrorServiceException {
        validar(email, tipoContacto, observacion);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarContactoCorreoElectronico(String id, String email, TipoContacto tipoContacto,
            String observacion) throws ErrorServiceException {
        ContactoCorreoElectronico contacto = buscarContactoCorreoElectronico(id);
        validar(email, tipoContacto, observacion);
        asignarCorreo(contacto, email, tipoContacto, observacion);
        repository.save(contacto);
    }

    public ContactoCorreoElectronico buscarContactoCorreoElectronico(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El contacto no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El contacto no existe o fue eliminado."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarContactoCorreoElectronico(String id) throws ErrorServiceException {
        ContactoCorreoElectronico contacto = buscarContactoCorreoElectronico(id);
        contacto.setEliminado(true);
        repository.save(contacto);
    }

    public List<ContactoCorreoElectronico> listarContactoCorreoElectronico() {
        return repository.findAllByOrderByEmailAsc();
    }

    public List<ContactoCorreoElectronico> listarContactoCorreoElectronicoActivo() {
        return repository.findByEliminadoFalseOrderByEmailAsc();
    }

    private void asignarCorreo(ContactoCorreoElectronico contacto, String email, TipoContacto tipoContacto,
            String observacion) {
        contacto.setEmail(email.strip().toLowerCase());
        contacto.setTipoContacto(tipoContacto);
        contacto.setObservacion(opcional(observacion));
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
