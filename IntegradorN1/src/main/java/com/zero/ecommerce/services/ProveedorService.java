package com.zero.ecommerce.services;

import java.util.ArrayList;
import java.util.List;
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

@Service
@Transactional(readOnly = true)
public class ProveedorService {

    private static final int LARGO_MAXIMO_RAZON_SOCIAL = 150;

    private final ProveedorRepository repository;
    private final ContactoCorreoElectronicoService correoService;
    private final ContactoTelefonicoService telefonoService;
    private final ContactoService contactoService;

    public ProveedorService(ProveedorRepository repository, ContactoCorreoElectronicoService correoService,
            ContactoTelefonicoService telefonoService, ContactoService contactoService) {
        this.repository = repository;
        this.correoService = correoService;
        this.telefonoService = telefonoService;
        this.contactoService = contactoService;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Proveedor crearProveedor(String razonSocial, List<ContactoItemDTO> contactos) throws ErrorServiceException {
        validar(razonSocial, contactos, null);
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial(razonSocial.strip());

        List<Contacto> listaContactos = new ArrayList<>();
        for (ContactoItemDTO item : filtrarContactosValidos(contactos)) {
            TipoContacto tipoContacto = parsearTipoContacto(item.getTipoContacto());
            if ("CORREO".equalsIgnoreCase(item.getTipo()) || "EMAIL".equalsIgnoreCase(item.getTipo())) {
                ContactoCorreoElectronico correo = correoService.crearContactoCorreoElectronico(
                        item.getValor(), tipoContacto, item.getObservacion());
                listaContactos.add(correo);
            } else if ("CELULAR".equalsIgnoreCase(item.getTipo())) {
                ContactoTelefonico tel = telefonoService.crearContactoTelefonico(
                        item.getValor(), TipoTelefono.CELULAR, tipoContacto, item.getObservacion());
                listaContactos.add(tel);
            } else if ("FIJO".equalsIgnoreCase(item.getTipo())) {
                ContactoTelefonico tel = telefonoService.crearContactoTelefonico(
                        item.getValor(), TipoTelefono.FIJO, tipoContacto, item.getObservacion());
                listaContactos.add(tel);
            }
        }
        proveedor.setContactos(listaContactos);
        return repository.save(proveedor);
    }

    public void validar(String razonSocial, List<ContactoItemDTO> contactos) throws ErrorServiceException {
        validar(razonSocial, contactos, null);
    }

    public void validar(String razonSocial, List<ContactoItemDTO> contactos, String idActual) throws ErrorServiceException {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new ErrorServiceException("La razón social es obligatoria.");
        }
        if (razonSocial.strip().length() > LARGO_MAXIMO_RAZON_SOCIAL) {
            throw new ErrorServiceException(
                    "La razón social no puede superar los " + LARGO_MAXIMO_RAZON_SOCIAL + " caracteres.");
        }
        Optional<Proveedor> dup = repository.findByRazonSocialIgnoreCaseAndEliminadoFalse(razonSocial.strip());
        if (dup.isPresent() && !dup.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe un proveedor con la razón social \"" + razonSocial.strip() + "\".");
        }

        List<ContactoItemDTO> validos = filtrarContactosValidos(contactos);
        int correos = 0;
        int celulares = 0;

        for (ContactoItemDTO item : validos) {
            TipoContacto tipoContacto = parsearTipoContacto(item.getTipoContacto());
            if ("CORREO".equalsIgnoreCase(item.getTipo()) || "EMAIL".equalsIgnoreCase(item.getTipo())) {
                correoService.validar(item.getValor(), tipoContacto, item.getObservacion());
                correos++;
            } else if ("CELULAR".equalsIgnoreCase(item.getTipo())) {
                telefonoService.validar(item.getValor(), TipoTelefono.CELULAR, tipoContacto, item.getObservacion());
                celulares++;
            } else if ("FIJO".equalsIgnoreCase(item.getTipo())) {
                telefonoService.validar(item.getValor(), TipoTelefono.FIJO, tipoContacto, item.getObservacion());
            } else {
                throw new ErrorServiceException("Tipo de contacto no válido: " + item.getTipo());
            }
        }

        if (correos == 0) {
            throw new ErrorServiceException("El proveedor debe tener al menos un correo electrónico con formato válido.");
        }
        if (celulares == 0) {
            throw new ErrorServiceException("El proveedor debe tener al menos un teléfono celular para WhatsApp.");
        }
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Proveedor modificarProveedor(String id, String razonSocial, List<ContactoItemDTO> contactos)
            throws ErrorServiceException {
        Proveedor proveedor = buscarProveedor(id);
        validar(razonSocial, contactos, id);
        proveedor.setRazonSocial(razonSocial.strip());

        // Marcar los contactos existentes anteriores como eliminados
        for (Contacto c : proveedor.getContactos()) {
            if (!c.isEliminado()) {
                contactoService.eliminarContacto(c.getId());
            }
        }

        // Crear y asignar los nuevos contactos
        List<Contacto> nuevaLista = new ArrayList<>();
        for (ContactoItemDTO item : filtrarContactosValidos(contactos)) {
            TipoContacto tipoContacto = parsearTipoContacto(item.getTipoContacto());
            if ("CORREO".equalsIgnoreCase(item.getTipo()) || "EMAIL".equalsIgnoreCase(item.getTipo())) {
                ContactoCorreoElectronico correo = correoService.crearContactoCorreoElectronico(
                        item.getValor(), tipoContacto, item.getObservacion());
                nuevaLista.add(correo);
            } else if ("CELULAR".equalsIgnoreCase(item.getTipo())) {
                ContactoTelefonico tel = telefonoService.crearContactoTelefonico(
                        item.getValor(), TipoTelefono.CELULAR, tipoContacto, item.getObservacion());
                nuevaLista.add(tel);
            } else if ("FIJO".equalsIgnoreCase(item.getTipo())) {
                ContactoTelefonico tel = telefonoService.crearContactoTelefonico(
                        item.getValor(), TipoTelefono.FIJO, tipoContacto, item.getObservacion());
                nuevaLista.add(tel);
            }
        }
        proveedor.setContactos(nuevaLista);
        return repository.save(proveedor);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarProveedor(String id) throws ErrorServiceException {
        Proveedor proveedor = buscarProveedor(id);
        proveedor.setEliminado(true);
        for (Contacto c : proveedor.getContactos()) {
            if (!c.isEliminado()) {
                contactoService.eliminarContacto(c.getId());
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

    public Optional<Proveedor> buscarProveedorPorRazonSocial(String razonSocial) {
        if (razonSocial == null || razonSocial.isBlank()) {
            return Optional.empty();
        }
        return repository.findByRazonSocialIgnoreCaseAndEliminadoFalse(razonSocial.strip());
    }

    public List<Proveedor> listarProveedor() {
        return repository.findAllByOrderByRazonSocialAsc();
    }

    public List<Proveedor> listarProveedorActivo() {
        return repository.findByEliminadoFalseOrderByRazonSocialAsc();
    }

    public List<Proveedor> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarProveedorActivo();
        }
        return repository.findByRazonSocialContainingIgnoreCaseAndEliminadoFalseOrderByRazonSocialAsc(texto.strip());
    }

    private List<ContactoItemDTO> filtrarContactosValidos(List<ContactoItemDTO> contactos) {
        if (contactos == null) {
            return List.of();
        }
        return contactos.stream()
                .filter(c -> c != null && c.getValor() != null && !c.getValor().isBlank())
                .toList();
    }

    private TipoContacto parsearTipoContacto(String tipoContactoStr) {
        if (tipoContactoStr == null || tipoContactoStr.isBlank()) {
            return TipoContacto.EMPRESA;
        }
        try {
            return TipoContacto.valueOf(tipoContactoStr.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoContacto.EMPRESA;
        }
    }
}
