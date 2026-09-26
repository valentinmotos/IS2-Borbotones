package com.zero.ecommerce.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.ContactoItemDTO;
import com.zero.ecommerce.entities.Contacto;
import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProveedorRepository;

/**
 * ABM de proveedores sobre la razón social y sus contactos (RF24). Los contactos se crean, modifican y dan de
 * baja con {@link ContactoService}, el mismo que usan Empresa y Cliente.
 */
@Service
@Transactional(readOnly = true)
public class ProveedorService {

    private static final int LARGO_MAXIMO_RAZON_SOCIAL = 150;
    private static final int LARGO_MINIMO_CELULAR = 11;
    private static final int LARGO_MAXIMO_CELULAR = 15;
    private static final int LARGO_CELULAR_ARGENTINO = 13;

    private final ProveedorRepository repository;
    private final ContactoService contactoService;

    public ProveedorService(ProveedorRepository repository, ContactoService contactoService) {
        this.repository = repository;
        this.contactoService = contactoService;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Proveedor crearProveedor(String razonSocial, List<ContactoItemDTO> contactos) throws ErrorServiceException {
        validar(razonSocial, contactos, null);
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial(razonSocial.strip());
        for (ContactoItemDTO item : sinFilasVacias(contactos)) {
            proveedor.getContactos().add(crearContacto(item));
        }
        return repository.save(proveedor);
    }

    public void validar(String razonSocial, List<ContactoItemDTO> contactos) throws ErrorServiceException {
        validar(razonSocial, contactos, null);
    }

    private void validar(String razonSocial, List<ContactoItemDTO> contactos, String idActual)
            throws ErrorServiceException {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new ErrorServiceException("La razón social es obligatoria.");
        }
        if (razonSocial.strip().length() > LARGO_MAXIMO_RAZON_SOCIAL) {
            throw new ErrorServiceException(
                    "La razón social no puede superar los " + LARGO_MAXIMO_RAZON_SOCIAL + " caracteres.");
        }
        Optional<Proveedor> duplicado = repository.findByRazonSocialIgnoreCaseAndEliminadoFalse(razonSocial.strip());
        if (duplicado.isPresent() && !duplicado.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe un proveedor con esa razón social.");
        }

        boolean tieneCorreo = false;
        boolean tieneCelular = false;
        for (ContactoItemDTO item : sinFilasVacias(contactos)) {
            validarContacto(item);
            tieneCorreo |= ContactoItemDTO.CORREO.equals(item.getTipo());
            tieneCelular |= ContactoItemDTO.CELULAR.equals(item.getTipo());
        }
        if (!tieneCorreo) {
            throw new ErrorServiceException("El proveedor tiene que tener al menos un correo electrónico.");
        }
        if (!tieneCelular) {
            throw new ErrorServiceException("El proveedor tiene que tener al menos un teléfono celular para WhatsApp.");
        }
    }

    private void validarContacto(ContactoItemDTO item) throws ErrorServiceException {
        TipoContacto tipoContacto = convertirTipoContacto(item.getTipoContacto());
        String tipo = item.getTipo() == null ? "" : item.getTipo();
        switch (tipo) {
            case ContactoItemDTO.CORREO -> contactoService.validarContactoCorreoElectronico(item.getValor(),
                    tipoContacto, item.getObservacion());
            case ContactoItemDTO.CELULAR -> {
                contactoService.validarContactoTelefonico(item.getValor(), TipoTelefono.CELULAR, tipoContacto,
                        item.getObservacion());
                validarCelularInternacional(item.getValor());
            }
            case ContactoItemDTO.FIJO -> contactoService.validarContactoTelefonico(item.getValor(), TipoTelefono.FIJO,
                    tipoContacto, item.getObservacion());
            default -> throw new ErrorServiceException(
                    "Cada contacto tiene que ser un correo electrónico, un celular o un teléfono fijo.");
        }
    }

    /**
     * WhatsApp necesita el número completo con código de país: sin 0 adelante y entre 11 y 15 dígitos. Los
     * celulares argentinos van con 549 + código de área + número, sin el 0 ni el 15 (ej: 5492614123456).
     */
    private void validarCelularInternacional(String celular) throws ErrorServiceException {
        String digitos = normalizarCelular(celular);
        if (digitos.startsWith("0") || digitos.length() < LARGO_MINIMO_CELULAR
                || digitos.length() > LARGO_MAXIMO_CELULAR) {
            throw new ErrorServiceException("El celular " + celular.strip()
                    + " tiene que estar en formato internacional, con código de país y de área (ej: 5492614123456).");
        }
        if (digitos.startsWith("54") && (!digitos.startsWith("549") || digitos.length() != LARGO_CELULAR_ARGENTINO)) {
            throw new ErrorServiceException("El celular " + celular.strip()
                    + " no es válido: los celulares de Argentina van con 549, el código de área y el número, sin 0 ni 15 (ej: 5492614123456).");
        }
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Proveedor modificarProveedor(String id, String razonSocial, List<ContactoItemDTO> contactos)
            throws ErrorServiceException {
        Proveedor proveedor = buscarProveedor(id);
        validar(razonSocial, contactos, id);
        proveedor.setRazonSocial(razonSocial.strip());

        // Las filas que traen el id de un contacto activo del proveedor lo modifican; las demás crean uno nuevo.
        // Un id ajeno al proveedor se ignora, así no se puede tocar el contacto de otro.
        Map<String, Contacto> actuales = new HashMap<>();
        for (Contacto contacto : proveedor.getContactos()) {
            if (!contacto.isEliminado()) {
                actuales.put(contacto.getId(), contacto);
            }
        }
        for (ContactoItemDTO item : sinFilasVacias(contactos)) {
            Contacto actual = item.getId() == null ? null : actuales.remove(item.getId());
            if (actual != null && mismaClase(actual, item)) {
                modificarContacto(actual, item);
            } else {
                // Un correo no puede pasar a ser teléfono (son clases distintas): se da de baja y se crea otro.
                if (actual != null) {
                    contactoService.eliminarContacto(actual.getId());
                }
                proveedor.getContactos().add(crearContacto(item));
            }
        }
        // Las filas que se quitaron del formulario se dan de baja.
        for (Contacto quitado : actuales.values()) {
            contactoService.eliminarContacto(quitado.getId());
        }
        return repository.save(proveedor);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarProveedor(String id) throws ErrorServiceException {
        Proveedor proveedor = buscarProveedor(id);
        proveedor.setEliminado(true);
        for (Contacto contacto : proveedor.getContactos()) {
            if (!contacto.isEliminado()) {
                contactoService.eliminarContacto(contacto.getId());
            }
        }
        repository.save(proveedor);
    }

    public Proveedor buscarProveedor(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El proveedor no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El proveedor no existe o fue eliminado."));
    }

    public List<Proveedor> listarProveedor() {
        return repository.findAllByOrderByRazonSocialAsc();
    }

    public List<Proveedor> listarProveedorActivo() {
        return repository.findByEliminadoFalseOrderByRazonSocialAsc();
    }

    /** Proveedores activos cuya razón social contiene el texto. Sin texto, todos los activos. */
    public List<Proveedor> listarProveedorActivo(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarProveedorActivo();
        }
        return repository.findByRazonSocialContainingIgnoreCaseAndEliminadoFalseOrderByRazonSocialAsc(texto.strip());
    }

    private Contacto crearContacto(ContactoItemDTO item) throws ErrorServiceException {
        TipoContacto tipoContacto = convertirTipoContacto(item.getTipoContacto());
        return switch (item.getTipo()) {
            case ContactoItemDTO.CORREO -> contactoService.crearContactoCorreoElectronico(item.getValor(),
                    tipoContacto, item.getObservacion());
            case ContactoItemDTO.CELULAR -> contactoService.crearContactoTelefonico(normalizarCelular(item.getValor()),
                    TipoTelefono.CELULAR, tipoContacto, item.getObservacion());
            default -> contactoService.crearContactoTelefonico(item.getValor(), TipoTelefono.FIJO, tipoContacto,
                    item.getObservacion());
        };
    }

    private void modificarContacto(Contacto contacto, ContactoItemDTO item) throws ErrorServiceException {
        TipoContacto tipoContacto = convertirTipoContacto(item.getTipoContacto());
        switch (item.getTipo()) {
            case ContactoItemDTO.CORREO -> contactoService.modificarContactoCorreoElectronico(contacto.getId(),
                    item.getValor(), tipoContacto, item.getObservacion());
            case ContactoItemDTO.CELULAR -> contactoService.modificarContactoTelefonico(contacto.getId(),
                    normalizarCelular(item.getValor()), TipoTelefono.CELULAR, tipoContacto, item.getObservacion());
            default -> contactoService.modificarContactoTelefonico(contacto.getId(), item.getValor(),
                    TipoTelefono.FIJO, tipoContacto, item.getObservacion());
        }
    }

    private boolean mismaClase(Contacto contacto, ContactoItemDTO item) {
        boolean esCorreo = ContactoItemDTO.CORREO.equals(item.getTipo());
        return esCorreo ? contacto instanceof ContactoCorreoElectronico : contacto instanceof ContactoTelefonico;
    }

    /** El celular se guarda solo con dígitos, como lo pide wa.me: "+54 9 261 412-3456" → "5492614123456". */
    private String normalizarCelular(String celular) {
        return celular.replaceAll("\\D", "");
    }

    private TipoContacto convertirTipoContacto(String tipoContacto) throws ErrorServiceException {
        if (tipoContacto == null || tipoContacto.isBlank()) {
            return null; // ContactoService avisa que es obligatorio
        }
        try {
            return TipoContacto.valueOf(tipoContacto.strip());
        } catch (IllegalArgumentException e) {
            throw new ErrorServiceException("El tipo de contacto no es válido.");
        }
    }

    // Spring deja en null los índices que faltan (contactos[0], contactos[2]) si se quitó una fila del medio.
    private List<ContactoItemDTO> sinFilasVacias(List<ContactoItemDTO> contactos) {
        if (contactos == null) {
            return List.of();
        }
        return contactos.stream().filter(item -> item != null).toList();
    }
}
