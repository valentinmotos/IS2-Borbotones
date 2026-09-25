package com.zero.ecommerce.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.ConfiguracionCorreoEmpresa;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ConfiguracionCorreoEmpresaRepository;
import com.zero.ecommerce.utils.TextoUtils;

/**
 * Cuenta SMTP con la que la empresa envía sus correos. Los nombres del diagrama dicen
 * "ConfiguracionCorreoAutomatico": se usa el nombre real de la entidad.
 * La clave se guarda tal cual porque hay que usarla para autenticarse; nunca se envía a la vista.
 */
@Service
@Transactional(readOnly = true)
public class ConfiguracionCorreoEmpresaService {

    private static final int LARGO_MAXIMO = 150;

    private final ConfiguracionCorreoEmpresaRepository repository;
    private final EmpresaService empresaService;

    public ConfiguracionCorreoEmpresaService(ConfiguracionCorreoEmpresaRepository repository,
            EmpresaService empresaService) {
        this.repository = repository;
        this.empresaService = empresaService;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public ConfiguracionCorreoEmpresa crearConfiguracionCorreoEmpresa(String correo, String clave, String puerto,
            String smtp, boolean tls, String idEmpresa) throws ErrorServiceException {
        validar(correo, clave, puerto, smtp, tls, idEmpresa);
        Empresa empresa = empresaService.buscarEmpresa(idEmpresa);
        if (repository.findByEmpresa_IdAndEliminadoFalse(empresa.getId()).isPresent()) {
            throw new ErrorServiceException("La empresa ya tiene una configuración de correo.");
        }
        ConfiguracionCorreoEmpresa configuracion = new ConfiguracionCorreoEmpresa();
        asignar(configuracion, correo, puerto, smtp, tls, empresa);
        configuracion.setClave(clave);
        return repository.save(configuracion);
    }

    public void validar(String correo, String clave, String puerto, String smtp, boolean tls, String idEmpresa)
            throws ErrorServiceException {
        if (clave == null || clave.isBlank()) {
            throw new ErrorServiceException("La clave del correo es obligatoria.");
        }
        validarSinClave(correo, puerto, smtp, idEmpresa);
    }

    private void validarSinClave(String correo, String puerto, String smtp, String idEmpresa)
            throws ErrorServiceException {
        if (correo == null || correo.isBlank()) {
            throw new ErrorServiceException("El correo es obligatorio.");
        }
        if (correo.strip().length() > LARGO_MAXIMO || !TextoUtils.esCorreoValido(correo)) {
            throw new ErrorServiceException("El correo no tiene un formato válido.");
        }
        if (smtp == null || smtp.isBlank()) {
            throw new ErrorServiceException("El servidor SMTP es obligatorio.");
        }
        if (smtp.strip().length() > LARGO_MAXIMO || smtp.strip().contains(" ")) {
            throw new ErrorServiceException("El servidor SMTP no es válido (ejemplo: smtp.gmail.com).");
        }
        validarPuerto(puerto);
        if (idEmpresa == null || idEmpresa.isBlank()) {
            throw new ErrorServiceException("La empresa es obligatoria.");
        }
        empresaService.buscarEmpresa(idEmpresa);
    }

    private void validarPuerto(String puerto) throws ErrorServiceException {
        if (puerto == null || puerto.isBlank()) {
            throw new ErrorServiceException("El puerto es obligatorio.");
        }
        int numero;
        try {
            numero = Integer.parseInt(puerto.strip());
        } catch (NumberFormatException e) {
            throw new ErrorServiceException("El puerto tiene que ser un número entre 1 y 65535.");
        }
        if (numero < 1 || numero > 65535) {
            throw new ErrorServiceException("El puerto tiene que ser un número entre 1 y 65535.");
        }
    }

    private void asignar(ConfiguracionCorreoEmpresa configuracion, String correo, String puerto, String smtp,
            boolean tls, Empresa empresa) {
        configuracion.setCorreo(correo.strip());
        configuracion.setPuerto(puerto.strip());
        configuracion.setSmtp(smtp.strip());
        configuracion.setTls(tls);
        configuracion.setEmpresa(empresa);
    }

    /** Si la clave llega vacía se conserva la guardada: el formulario nunca la muestra. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarConfiguracionCorreoEmpresa(String id, String correo, String clave, String puerto,
            String smtp, boolean tls, String idEmpresa) throws ErrorServiceException {
        ConfiguracionCorreoEmpresa configuracion = buscarConfiguracionCorreoEmpresa(id);
        validarSinClave(correo, puerto, smtp, idEmpresa);
        Empresa empresa = empresaService.buscarEmpresa(idEmpresa);
        Optional<ConfiguracionCorreoEmpresa> otra = repository.findByEmpresa_IdAndEliminadoFalse(empresa.getId());
        if (otra.isPresent() && !otra.get().getId().equals(id)) {
            throw new ErrorServiceException("La empresa ya tiene una configuración de correo.");
        }
        asignar(configuracion, correo, puerto, smtp, tls, empresa);
        if (clave != null && !clave.isBlank()) {
            configuracion.setClave(clave);
        }
        repository.save(configuracion);
    }

    public ConfiguracionCorreoEmpresa buscarConfiguracionCorreoEmpresa(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La configuración de correo no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La configuración de correo no existe o fue eliminada."));
    }

    /** La configuración con la que se envían los correos: la de la sede central. */
    public ConfiguracionCorreoEmpresa buscarConfiguracionCorreoEmpresa() throws ErrorServiceException {
        Empresa sede = empresaService.buscarSedeCentral();
        return repository.findByEmpresa_IdAndEliminadoFalse(sede.getId())
                .orElseThrow(() -> new ErrorServiceException("Todavía no se configuró el correo de la empresa."));
    }

    /** Igual que la anterior, pero vacía si todavía no hay configuración (para la pantalla). */
    public Optional<ConfiguracionCorreoEmpresa> encontrarConfiguracionCorreoEmpresa() throws ErrorServiceException {
        Empresa sede = empresaService.buscarSedeCentral();
        return repository.findByEmpresa_IdAndEliminadoFalse(sede.getId());
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarConfiguracionCorreoEmpresa(String id) throws ErrorServiceException {
        ConfiguracionCorreoEmpresa configuracion = buscarConfiguracionCorreoEmpresa(id);
        configuracion.setEliminado(true);
        repository.save(configuracion);
    }
}
