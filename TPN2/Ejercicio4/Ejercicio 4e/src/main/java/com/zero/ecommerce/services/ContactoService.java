package com.zero.ecommerce.services;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Contacto;
import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ContactoCorreoElectronicoRepository;
import com.zero.ecommerce.repositories.ContactoRepository;
import com.zero.ecommerce.repositories.ContactoTelefonicoRepository;
import com.zero.ecommerce.utils.TextoUtils;

/**
 * Un service para toda la jerarquía Contacto (correo y teléfono), que comparten la baja y la búsqueda.
 * Lo usan Empresa (E1-07) y Proveedor (E3-01).
 */
@Service
@Transactional(readOnly = true)
public class ContactoService {

    private static final int LARGO_MAXIMO_CORREO = 150;
    private static final int LARGO_MAXIMO_OBSERVACION = 255;
    // Dígitos con espacios, guiones, paréntesis y un + inicial opcional: "+54 261 423-1234".
    private static final Pattern TELEFONO = Pattern.compile("\\+?[\\d ()-]{6,25}");

    private final ContactoRepository repository;
    private final ContactoCorreoElectronicoRepository correoRepository;
    private final ContactoTelefonicoRepository telefonoRepository;

    public ContactoService(ContactoRepository repository, ContactoCorreoElectronicoRepository correoRepository,
            ContactoTelefonicoRepository telefonoRepository) {
        this.repository = repository;
        this.correoRepository = correoRepository;
        this.telefonoRepository = telefonoRepository;
    }

    // ----- Correo electrónico -----

    @Transactional(rollbackFor = ErrorServiceException.class)
    public ContactoCorreoElectronico crearContactoCorreoElectronico(String email, TipoContacto tipoContacto,
            String observacion) throws ErrorServiceException {
        validarContactoCorreoElectronico(email, tipoContacto, observacion);
        ContactoCorreoElectronico contacto = new ContactoCorreoElectronico();
        asignarCorreo(contacto, email, tipoContacto, observacion);
        return correoRepository.save(contacto);
    }

    public void validarContactoCorreoElectronico(String email, TipoContacto tipoContacto, String observacion)
            throws ErrorServiceException {
        if (email == null || email.isBlank()) {
            throw new ErrorServiceException("El correo electrónico es obligatorio.");
        }
        if (email.strip().length() > LARGO_MAXIMO_CORREO || !TextoUtils.esCorreoValido(email)) {
            throw new ErrorServiceException("El correo electrónico no tiene un formato válido.");
        }
        validarComunes(tipoContacto, observacion);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarContactoCorreoElectronico(String id, String email, TipoContacto tipoContacto,
            String observacion) throws ErrorServiceException {
        ContactoCorreoElectronico contacto = correoRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El contacto no existe o fue eliminado."));
        validarContactoCorreoElectronico(email, tipoContacto, observacion);
        asignarCorreo(contacto, email, tipoContacto, observacion);
        correoRepository.save(contacto);
    }

    private void asignarCorreo(ContactoCorreoElectronico contacto, String email, TipoContacto tipoContacto,
            String observacion) {
        contacto.setEmail(email.strip().toLowerCase());
        contacto.setTipoContacto(tipoContacto);
        contacto.setObservacion(opcional(observacion));
    }

    public List<ContactoCorreoElectronico> listarContactoCorreoElectronico() {
        return correoRepository.findAllByOrderByEmailAsc();
    }

    public List<ContactoCorreoElectronico> listarContactoCorreoElectronicoActivo() {
        return correoRepository.findByEliminadoFalseOrderByEmailAsc();
    }

    // ----- Teléfono -----

    @Transactional(rollbackFor = ErrorServiceException.class)
    public ContactoTelefonico crearContactoTelefonico(String telefono, TipoTelefono tipoTelefono,
            TipoContacto tipoContacto, String observacion) throws ErrorServiceException {
        validarContactoTelefonico(telefono, tipoTelefono, tipoContacto, observacion);
        ContactoTelefonico contacto = new ContactoTelefonico();
        asignarTelefono(contacto, telefono, tipoTelefono, tipoContacto, observacion);
        return telefonoRepository.save(contacto);
    }

    public void validarContactoTelefonico(String telefono, TipoTelefono tipoTelefono, TipoContacto tipoContacto,
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
        validarComunes(tipoContacto, observacion);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarContactoTelefonico(String id, String telefono, TipoTelefono tipoTelefono,
            TipoContacto tipoContacto, String observacion) throws ErrorServiceException {
        ContactoTelefonico contacto = telefonoRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El contacto no existe o fue eliminado."));
        validarContactoTelefonico(telefono, tipoTelefono, tipoContacto, observacion);
        asignarTelefono(contacto, telefono, tipoTelefono, tipoContacto, observacion);
        telefonoRepository.save(contacto);
    }

    private void asignarTelefono(ContactoTelefonico contacto, String telefono, TipoTelefono tipoTelefono,
            TipoContacto tipoContacto, String observacion) {
        contacto.setTelefono(telefono.strip());
        contacto.setTipoTelefono(tipoTelefono);
        contacto.setTipoContacto(tipoContacto);
        contacto.setObservacion(opcional(observacion));
    }

    public List<ContactoTelefonico> listarContactoTelefonico() {
        return telefonoRepository.findAllByOrderByTelefonoAsc();
    }

    public List<ContactoTelefonico> listarContactoTelefonicoActivo() {
        return telefonoRepository.findByEliminadoFalseOrderByTelefonoAsc();
    }

    // ----- Comunes a la jerarquía -----

    public Contacto buscarContacto(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El contacto no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El contacto no existe o fue eliminado."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarContacto(String id) throws ErrorServiceException {
        Contacto contacto = buscarContacto(id);
        contacto.setEliminado(true);
        repository.save(contacto);
    }

    private void validarComunes(TipoContacto tipoContacto, String observacion) throws ErrorServiceException {
        if (tipoContacto == null) {
            throw new ErrorServiceException("El tipo de contacto es obligatorio.");
        }
        if (observacion != null && observacion.strip().length() > LARGO_MAXIMO_OBSERVACION) {
            throw new ErrorServiceException(
                    "La observación del contacto no puede superar los " + LARGO_MAXIMO_OBSERVACION + " caracteres.");
        }
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
