package com.zero.ecommerce.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.ContactoCorreoElectronico;
import com.zero.ecommerce.entities.ContactoTelefonico;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoEmpresa;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.EmpresaRepository;
import com.zero.ecommerce.utils.CuitUtils;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class EmpresaService {

    private static final int LARGO_MAXIMO_RAZON_SOCIAL = 150;

    private final EmpresaRepository repository;
    private final DireccionService direccionService;
    private final ContactoService contactoService;

    public EmpresaService(EmpresaRepository repository, DireccionService direccionService,
            ContactoService contactoService) {
        this.repository = repository;
        this.direccionService = direccionService;
        this.contactoService = contactoService;
    }

    /**
     * Crea la empresa con su dirección y sus contactos (un correo y un teléfono). Devuelve la empresa
     * (el diagrama dice void) porque la configuración de correo y el seeder necesitan su id.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Empresa crearEmpresa(String razonSocial, String cuit, TipoEmpresa tipoSucursal, DireccionForm direccion,
            String correo, String telefono, TipoTelefono tipoTelefono) throws ErrorServiceException {
        validar(razonSocial, cuit, tipoSucursal, direccion, correo, telefono, tipoTelefono, null);
        Empresa empresa = new Empresa();
        asignarDatos(empresa, razonSocial, cuit, tipoSucursal);
        empresa.setDireccion(direccionService.crearDireccion(direccion.getCalle(), direccion.getNumeracion(),
                direccion.getBarrio(), direccion.getManzanaPiso(), direccion.getCasaDepartamento(),
                direccion.getReferencia(), direccion.getLocalidadId()));
        empresa.getContactos().add(contactoService.crearContactoCorreoElectronico(correo, TipoContacto.EMPRESA, null));
        empresa.getContactos().add(
                contactoService.crearContactoTelefonico(telefono, tipoTelefono, TipoContacto.EMPRESA, null));
        return repository.save(empresa);
    }

    public void validar(String razonSocial, String cuit, TipoEmpresa tipoSucursal, DireccionForm direccion,
            String correo, String telefono, TipoTelefono tipoTelefono) throws ErrorServiceException {
        validar(razonSocial, cuit, tipoSucursal, direccion, correo, telefono, tipoTelefono, null);
    }

    private void validar(String razonSocial, String cuit, TipoEmpresa tipoSucursal, DireccionForm direccion,
            String correo, String telefono, TipoTelefono tipoTelefono, String idActual) throws ErrorServiceException {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new ErrorServiceException("La razón social es obligatoria.");
        }
        if (razonSocial.strip().length() > LARGO_MAXIMO_RAZON_SOCIAL) {
            throw new ErrorServiceException(
                    "La razón social no puede superar los " + LARGO_MAXIMO_RAZON_SOCIAL + " caracteres.");
        }
        validarCuit(cuit, idActual);
        validarTipo(tipoSucursal, idActual);
        if (direccion == null) {
            throw new ErrorServiceException("La dirección es obligatoria.");
        }
        direccionService.validar(direccion.getCalle(), direccion.getNumeracion(), direccion.getBarrio(),
                direccion.getManzanaPiso(), direccion.getCasaDepartamento(), direccion.getReferencia(),
                direccion.getLocalidadId());
        contactoService.validarContactoCorreoElectronico(correo, TipoContacto.EMPRESA, null);
        contactoService.validarContactoTelefonico(telefono, tipoTelefono, TipoContacto.EMPRESA, null);
    }

    private void validarCuit(String cuit, String idActual) throws ErrorServiceException {
        if (cuit == null || cuit.isBlank()) {
            throw new ErrorServiceException("El CUIT es obligatorio.");
        }
        if (!CuitUtils.esValido(cuit)) {
            throw new ErrorServiceException(
                    "El CUIT no es válido: tiene que tener 11 dígitos (XX-XXXXXXXX-X) y un dígito verificador correcto.");
        }
        Optional<Empresa> duplicada = repository.findByCuitAndEliminadoFalse(CuitUtils.normalizar(cuit));
        if (duplicada.isPresent() && !duplicada.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe una empresa con ese CUIT.");
        }
    }

    private void validarTipo(TipoEmpresa tipoSucursal, String idActual) throws ErrorServiceException {
        if (tipoSucursal == null) {
            throw new ErrorServiceException("El tipo de empresa es obligatorio.");
        }
        Optional<Empresa> sede = repository.findFirstByTipoSucursalAndEliminadoFalse(TipoEmpresa.SEDE_CENTRAL);
        boolean esLaSede = sede.isPresent() && sede.get().getId().equals(idActual);
        if (tipoSucursal == TipoEmpresa.SEDE_CENTRAL && sede.isPresent() && !esLaSede) {
            throw new ErrorServiceException("Ya existe una sede central. Solo puede haber una.");
        }
        if (tipoSucursal == TipoEmpresa.SUCURSAL && esLaSede) {
            throw new ErrorServiceException("La sede central no puede pasar a ser sucursal.");
        }
    }

    private void asignarDatos(Empresa empresa, String razonSocial, String cuit, TipoEmpresa tipoSucursal) {
        empresa.setRazonSocial(razonSocial.strip());
        empresa.setCuit(CuitUtils.normalizar(cuit));
        empresa.setTipoSucursal(tipoSucursal);
    }

    public Empresa buscarEmpresa(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La empresa no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La empresa no existe o fue eliminada."));
    }

    /** Busca por razón social, sin importar mayúsculas ni tildes. */
    public Empresa buscarEmpresaPorNombre(String nombre) throws ErrorServiceException {
        if (nombre == null || nombre.isBlank()) {
            throw new ErrorServiceException("La razón social es obligatoria.");
        }
        return listarEmpresaActiva().stream()
                .filter(e -> TextoUtils.mismoNombre(e.getRazonSocial(), nombre))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("La empresa no existe o fue eliminada."));
    }

    /** La empresa que envía los correos y a la que pertenece todo el stock. */
    public Empresa buscarSedeCentral() throws ErrorServiceException {
        return repository.findFirstByTipoSucursalAndEliminadoFalse(TipoEmpresa.SEDE_CENTRAL)
                .orElseThrow(() -> new ErrorServiceException("Todavía no se cargó la empresa sede central."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarEmpresa(String id, String razonSocial, String cuit, TipoEmpresa tipoSucursal,
            DireccionForm direccion, String correo, String telefono, TipoTelefono tipoTelefono)
            throws ErrorServiceException {
        Empresa empresa = buscarEmpresa(id);
        validar(razonSocial, cuit, tipoSucursal, direccion, correo, telefono, tipoTelefono, id);
        asignarDatos(empresa, razonSocial, cuit, tipoSucursal);
        modificarDireccion(empresa, direccion);
        modificarContactos(empresa, correo, telefono, tipoTelefono);
        repository.save(empresa);
    }

    private void modificarDireccion(Empresa empresa, DireccionForm d) throws ErrorServiceException {
        if (empresa.getDireccion() == null) {
            empresa.setDireccion(direccionService.crearDireccion(d.getCalle(), d.getNumeracion(), d.getBarrio(),
                    d.getManzanaPiso(), d.getCasaDepartamento(), d.getReferencia(), d.getLocalidadId()));
        } else {
            direccionService.modificarDireccion(empresa.getDireccion().getId(), d.getCalle(), d.getNumeracion(),
                    d.getBarrio(), d.getManzanaPiso(), d.getCasaDepartamento(), d.getReferencia(),
                    d.getLocalidadId());
        }
    }

    private void modificarContactos(Empresa empresa, String correo, String telefono, TipoTelefono tipoTelefono)
            throws ErrorServiceException {
        Optional<ContactoCorreoElectronico> correoActual = empresa.buscarCorreoActivo();
        if (correoActual.isPresent()) {
            contactoService.modificarContactoCorreoElectronico(correoActual.get().getId(), correo,
                    TipoContacto.EMPRESA, correoActual.get().getObservacion());
        } else {
            empresa.getContactos()
                    .add(contactoService.crearContactoCorreoElectronico(correo, TipoContacto.EMPRESA, null));
        }
        Optional<ContactoTelefonico> telefonoActual = empresa.buscarTelefonoActivo();
        if (telefonoActual.isPresent()) {
            contactoService.modificarContactoTelefonico(telefonoActual.get().getId(), telefono, tipoTelefono,
                    TipoContacto.EMPRESA, telefonoActual.get().getObservacion());
        } else {
            empresa.getContactos().add(
                    contactoService.crearContactoTelefonico(telefono, tipoTelefono, TipoContacto.EMPRESA, null));
        }
    }

    /** Baja lógica de la empresa, su dirección y sus contactos. La sede central no se puede eliminar. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarEmpresa(String id) throws ErrorServiceException {
        Empresa empresa = buscarEmpresa(id);
        if (empresa.getTipoSucursal() == TipoEmpresa.SEDE_CENTRAL) {
            throw new ErrorServiceException("No se puede eliminar la sede central.");
        }
        if (empresa.getDireccion() != null && !empresa.getDireccion().isEliminado()) {
            direccionService.eliminarDireccion(empresa.getDireccion().getId());
        }
        for (var contacto : empresa.getContactos()) {
            if (!contacto.isEliminado()) {
                contactoService.eliminarContacto(contacto.getId());
            }
        }
        empresa.setEliminado(true);
        repository.save(empresa);
    }

    public List<Empresa> listarEmpresa() {
        return repository.findAllByOrderByRazonSocialAsc();
    }

    public List<Empresa> listarEmpresaActiva() {
        return repository.findByEliminadoFalseOrderByRazonSocialAsc();
    }

    /** Filas del listado del ABM: razón social, CUIT, tipo y localidad. */
    public List<FilaTablaDTO> listarFilaEmpresaActiva() {
        return listarEmpresaActiva().stream()
                .map(e -> new FilaTablaDTO(e.getId(), e.getRazonSocial(), List.of(e.getRazonSocial(), e.getCuit(),
                        e.getTipoSucursal().getDescripcion(),
                        e.getDireccion() == null ? "-" : e.getDireccion().getLocalidad().getNombre())))
                .toList();
    }

    /**
     * Datos de la sede central para el pie de los correos, como texto plano: así el template no
     * necesita cargar relaciones perezosas fuera de la transacción (EmailService envía en otro hilo).
     */
    public Map<String, String> listarDatoCorreoSedeCentral() {
        Map<String, String> datos = new LinkedHashMap<>();
        Optional<Empresa> sede = repository.findFirstByTipoSucursalAndEliminadoFalse(TipoEmpresa.SEDE_CENTRAL);
        // Las cuatro claves están siempre (con null si falta el dato) para que el template pueda preguntar por ellas.
        datos.put("razonSocial", sede.map(Empresa::getRazonSocial).orElse("Zero"));
        datos.put("direccion", sede.map(Empresa::getDireccion).map(Direccion::describir).orElse(null));
        datos.put("correo", sede.flatMap(Empresa::buscarCorreoActivo).map(ContactoCorreoElectronico::getEmail)
                .orElse(null));
        datos.put("telefono", sede.flatMap(Empresa::buscarTelefonoActivo).map(ContactoTelefonico::getTelefono)
                .orElse(null));
        return datos;
    }

    /** Tipos de empresa en el orden del enum: valor → descripción. */
    public Map<String, String> listarTipoEmpresa() {
        Map<String, String> tipos = new LinkedHashMap<>();
        for (TipoEmpresa tipo : TipoEmpresa.values()) {
            tipos.put(tipo.name(), tipo.getDescripcion());
        }
        return tipos;
    }

    /** Tipos de teléfono en el orden del enum: valor → descripción. */
    public Map<String, String> listarTipoTelefono() {
        Map<String, String> tipos = new LinkedHashMap<>();
        for (TipoTelefono tipo : TipoTelefono.values()) {
            tipos.put(tipo.name(), tipo.getDescripcion());
        }
        return tipos;
    }

    /** Convierte el valor del formulario al enum TipoEmpresa. */
    public TipoEmpresa convertirTipoEmpresa(String tipo) throws ErrorServiceException {
        if (tipo == null || tipo.isBlank()) {
            throw new ErrorServiceException("El tipo de empresa es obligatorio.");
        }
        for (TipoEmpresa valor : TipoEmpresa.values()) {
            if (valor.name().equals(tipo)) {
                return valor;
            }
        }
        throw new ErrorServiceException("El tipo de empresa no es válido.");
    }

    /** Convierte el valor del formulario al enum TipoTelefono. */
    public TipoTelefono convertirTipoTelefono(String tipo) throws ErrorServiceException {
        if (tipo == null || tipo.isBlank()) {
            throw new ErrorServiceException("El tipo de teléfono es obligatorio.");
        }
        for (TipoTelefono valor : TipoTelefono.values()) {
            if (valor.name().equals(tipo)) {
                return valor;
            }
        }
        throw new ErrorServiceException("El tipo de teléfono no es válido.");
    }
}
