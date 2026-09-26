package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.entities.enums.Sexo;
import com.zero.ecommerce.entities.enums.TipoContacto;
import com.zero.ecommerce.entities.enums.TipoDocumento;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.entities.enums.TipoTelefono;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ClienteRepository;
import com.zero.ecommerce.repositories.PersonaRepository;

/**
 * Perfil del cliente (RF03 y RF05). Según las decisiones de diseño, se ignora el tipoEmpleado del diagrama
 * y el correo electrónico es el nombre de usuario, así que no se pide. Se suman sexo (lo pide el enunciado),
 * la dirección (fragments/direccion) y la foto de perfil opcional.
 */
@Service
@Transactional(readOnly = true)
public class ClienteService {

    private static final int LARGO_MAXIMO_NOMBRE = 100;
    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 120;
    private static final Pattern DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern PASAPORTE = Pattern.compile("[A-Z0-9]{6,15}");

    private final ClienteRepository repository;
    private final PersonaRepository personaRepository;
    private final UsuarioService usuarioService;
    private final NacionalidadService nacionalidadService;
    private final DireccionService direccionService;
    private final ContactoService contactoService;
    private final ImagenService imagenService;

    public ClienteService(ClienteRepository repository, PersonaRepository personaRepository,
            UsuarioService usuarioService, NacionalidadService nacionalidadService, DireccionService direccionService,
            ContactoService contactoService, ImagenService imagenService) {
        this.repository = repository;
        this.personaRepository = personaRepository;
        this.usuarioService = usuarioService;
        this.nacionalidadService = nacionalidadService;
        this.direccionService = direccionService;
        this.contactoService = contactoService;
        this.imagenService = imagenService;
    }

    /**
     * Pantalla /cliente/perfil: la primera vez crea el cliente y lo asocia al usuario logueado; las siguientes
     * lo modifica. foto es opcional: si viene vacía se conserva la actual.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Cliente guardarPerfilCliente(String idUsuario, String nombre, String apellido, Sexo sexo,
            LocalDate fechaNacimiento, TipoDocumento tipoDocumento, String numeroDocumento, String telefono,
            DireccionForm direccion, String idNacionalidad, MultipartFile foto) throws ErrorServiceException {
        Usuario usuario = usuarioService.buscarUsuario(idUsuario);
        if (usuario.getRol() != RolUsuario.CLIENTE) {
            throw new ErrorServiceException("Solo los clientes tienen perfil de cliente.");
        }
        Optional<Cliente> existente = buscarClientePorUsuario(idUsuario);
        if (existente.isPresent()) {
            modificarCliente(existente.get().getId(), nombre, apellido, sexo, fechaNacimiento, tipoDocumento,
                    numeroDocumento, telefono, direccion, idNacionalidad, foto);
            return buscarCliente(existente.get().getId());
        }
        Cliente cliente = crearCliente(nombre, apellido, sexo, fechaNacimiento, tipoDocumento, numeroDocumento,
                telefono, direccion, idNacionalidad, foto);
        asociarClienteUsuario(cliente, usuario);
        return cliente;
    }

    /** Devuelve el cliente creado (el diagrama dice void) para poder asociarlo al usuario. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Cliente crearCliente(String nombre, String apellido, Sexo sexo, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, String telefono, DireccionForm direccion,
            String idNacionalidad, MultipartFile foto) throws ErrorServiceException {
        Nacionalidad nacionalidad = validar(nombre, apellido, sexo, fechaNacimiento, tipoDocumento, numeroDocumento,
                telefono, direccion, idNacionalidad, null);
        Cliente cliente = new Cliente();
        asignarDatos(cliente, nombre, apellido, sexo, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad);
        cliente.setDireccion(direccionService.crearDireccion(direccion.getCalle(), direccion.getNumeracion(),
                direccion.getBarrio(), direccion.getManzanaPiso(), direccion.getCasaDepartamento(),
                direccion.getReferencia(), direccion.getLocalidadId()));
        cliente.setTelefono(contactoService.crearContactoTelefonico(telefono, TipoTelefono.CELULAR,
                TipoContacto.PERSONAL, null));
        if (foto != null && !foto.isEmpty()) {
            cliente.setImagen(imagenService.crearImagen(foto, TipoImagen.PERSONA));
        }
        return repository.save(cliente);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarCliente(String id, String nombre, String apellido, Sexo sexo, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, String telefono, DireccionForm direccion,
            String idNacionalidad, MultipartFile foto) throws ErrorServiceException {
        Cliente cliente = buscarCliente(id);
        Nacionalidad nacionalidad = validar(nombre, apellido, sexo, fechaNacimiento, tipoDocumento, numeroDocumento,
                telefono, direccion, idNacionalidad, id);
        asignarDatos(cliente, nombre, apellido, sexo, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad);
        if (cliente.getDireccion() == null) {
            cliente.setDireccion(direccionService.crearDireccion(direccion.getCalle(), direccion.getNumeracion(),
                    direccion.getBarrio(), direccion.getManzanaPiso(), direccion.getCasaDepartamento(),
                    direccion.getReferencia(), direccion.getLocalidadId()));
        } else {
            direccionService.modificarDireccion(cliente.getDireccion().getId(), direccion.getCalle(),
                    direccion.getNumeracion(), direccion.getBarrio(), direccion.getManzanaPiso(),
                    direccion.getCasaDepartamento(), direccion.getReferencia(), direccion.getLocalidadId());
        }
        if (cliente.getTelefono() == null) {
            cliente.setTelefono(contactoService.crearContactoTelefonico(telefono, TipoTelefono.CELULAR,
                    TipoContacto.PERSONAL, null));
        } else {
            contactoService.modificarContactoTelefonico(cliente.getTelefono().getId(), telefono,
                    TipoTelefono.CELULAR, TipoContacto.PERSONAL, null);
        }
        if (foto != null && !foto.isEmpty()) {
            Imagen imagen = cliente.getImagen() == null ? imagenService.crearImagen(foto, TipoImagen.PERSONA)
                    : imagenService.modificarImagen(cliente.getImagen().getId(), foto, TipoImagen.PERSONA);
            cliente.setImagen(imagen);
        }
        repository.save(cliente);
    }

    /**
     * Valida todos los datos antes de guardar nada: obligatorios, mayor de edad, documento único, teléfono,
     * dirección y nacionalidad activa. idActual es el cliente que se modifica (null en el alta).
     * Devuelve la nacionalidad elegida.
     */
    public Nacionalidad validar(String nombre, String apellido, Sexo sexo, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, String telefono, DireccionForm direccion,
            String idNacionalidad, String idActual) throws ErrorServiceException {
        validarTexto(nombre, "El nombre");
        validarTexto(apellido, "El apellido");
        if (sexo == null) {
            throw new ErrorServiceException("El sexo es obligatorio.");
        }
        validarFechaNacimiento(fechaNacimiento);
        validarDocumento(tipoDocumento, numeroDocumento, idActual);
        if (idNacionalidad == null || idNacionalidad.isBlank()) {
            throw new ErrorServiceException("La nacionalidad es obligatoria.");
        }
        Nacionalidad nacionalidad = nacionalidadService.buscarNacionalidad(idNacionalidad);
        contactoService.validarContactoTelefonico(telefono, TipoTelefono.CELULAR, TipoContacto.PERSONAL, null);
        if (direccion == null) {
            throw new ErrorServiceException("La dirección es obligatoria.");
        }
        direccionService.validar(direccion.getCalle(), direccion.getNumeracion(), direccion.getBarrio(),
                direccion.getManzanaPiso(), direccion.getCasaDepartamento(), direccion.getReferencia(),
                direccion.getLocalidadId());
        return nacionalidad;
    }

    private void validarTexto(String valor, String campo) throws ErrorServiceException {
        if (valor == null || valor.isBlank()) {
            throw new ErrorServiceException(campo + " es obligatorio.");
        }
        if (valor.strip().length() > LARGO_MAXIMO_NOMBRE) {
            throw new ErrorServiceException(campo + " no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) throws ErrorServiceException {
        if (fechaNacimiento == null) {
            throw new ErrorServiceException("La fecha de nacimiento es obligatoria.");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (fechaNacimiento.isAfter(LocalDate.now()) || edad > EDAD_MAXIMA) {
            throw new ErrorServiceException("La fecha de nacimiento no es válida.");
        }
        if (edad < EDAD_MINIMA) {
            throw new ErrorServiceException("Tenés que ser mayor de edad para registrar tu perfil.");
        }
    }

    // El documento es único entre todas las personas activas (clientes y empleados), por tipo y número.
    private void validarDocumento(TipoDocumento tipoDocumento, String numeroDocumento, String idActual)
            throws ErrorServiceException {
        if (tipoDocumento == null) {
            throw new ErrorServiceException("El tipo de documento es obligatorio.");
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new ErrorServiceException("El número de documento es obligatorio.");
        }
        String numero = normalizarDocumento(numeroDocumento);
        if (tipoDocumento == TipoDocumento.DNI && !DNI.matcher(numero).matches()) {
            throw new ErrorServiceException("El DNI tiene que tener 7 u 8 números.");
        }
        if (tipoDocumento == TipoDocumento.PASAPORTE && !PASAPORTE.matcher(numero).matches()) {
            throw new ErrorServiceException("El pasaporte tiene que tener entre 6 y 15 letras o números.");
        }
        boolean repetido = personaRepository.findByTipoDocumentoAndNumeroDocumentoAndEliminadoFalse(tipoDocumento,
                numero).stream().anyMatch(p -> !p.getId().equals(idActual));
        if (repetido) {
            throw new ErrorServiceException("Ya hay otra persona registrada con ese documento.");
        }
    }

    // "30.111.222" → "30111222"; los pasaportes se guardan en mayúsculas.
    private String normalizarDocumento(String numeroDocumento) {
        return numeroDocumento.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }

    private void asignarDatos(Cliente cliente, String nombre, String apellido, Sexo sexo, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, Nacionalidad nacionalidad) {
        cliente.setNombre(nombre.strip());
        cliente.setApellido(apellido.strip());
        cliente.setSexo(sexo);
        cliente.setFechaNacimiento(fechaNacimiento);
        cliente.setTipoDocumento(tipoDocumento);
        cliente.setNumeroDocumento(normalizarDocumento(numeroDocumento));
        cliente.setNacionalidad(nacionalidad);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void asociarClienteUsuario(Cliente cliente, Usuario usuario) throws ErrorServiceException {
        if (cliente == null || usuario == null) {
            throw new ErrorServiceException("El cliente y el usuario son obligatorios.");
        }
        cliente.setUsuario(usuario);
        repository.save(cliente);
    }

    public Cliente buscarCliente(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El cliente no existe o fue eliminado.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El cliente no existe o fue eliminado."));
    }

    /** El cliente del usuario, si ya cargó su perfil. */
    public Optional<Cliente> buscarClientePorUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsuario_IdAndEliminadoFalse(idUsuario);
    }

    public List<Cliente> listarCliente() {
        return repository.findAllByOrderByApellidoAscNombreAsc();
    }

    public List<Cliente> listarClienteActivo() {
        return repository.findByEliminadoFalseOrderByApellidoAscNombreAsc();
    }

    /** Sexos en el orden del enum: valor → descripción. */
    public Map<String, String> listarSexo() {
        Map<String, String> sexos = new LinkedHashMap<>();
        for (Sexo sexo : Sexo.values()) {
            sexos.put(sexo.name(), sexo.getDescripcion());
        }
        return sexos;
    }

    /** Tipos de documento: valor → texto visible. */
    public Map<String, String> listarTipoDocumento() {
        Map<String, String> tipos = new LinkedHashMap<>();
        tipos.put(TipoDocumento.DNI.name(), "DNI");
        tipos.put(TipoDocumento.PASAPORTE.name(), "Pasaporte");
        return tipos;
    }

    /** Nacionalidades activas: id → nombre. */
    public Map<String, String> listarNacionalidad() {
        Map<String, String> nacionalidades = new LinkedHashMap<>();
        for (Nacionalidad nacionalidad : nacionalidadService.listarNacionalidadActiva()) {
            nacionalidades.put(nacionalidad.getId(), nacionalidad.getNombre());
        }
        return nacionalidades;
    }

    /** Convierte el valor del formulario al enum Sexo. Vacío devuelve null (lo rechaza validar). */
    public Sexo convertirSexo(String sexo) throws ErrorServiceException {
        if (sexo == null || sexo.isBlank()) {
            return null;
        }
        for (Sexo valor : Sexo.values()) {
            if (valor.name().equals(sexo)) {
                return valor;
            }
        }
        throw new ErrorServiceException("El sexo no es válido.");
    }

    /** Convierte el valor del formulario al enum TipoDocumento. Vacío devuelve null (lo rechaza validar). */
    public TipoDocumento convertirTipoDocumento(String tipo) throws ErrorServiceException {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        for (TipoDocumento valor : TipoDocumento.values()) {
            if (valor.name().equals(tipo)) {
                return valor;
            }
        }
        throw new ErrorServiceException("El tipo de documento no es válido.");
    }

    /** Convierte la fecha del input date (aaaa-mm-dd). Vacía devuelve null (lo rechaza validar). */
    public LocalDate convertirFechaNacimiento(String fecha) throws ErrorServiceException {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.strip());
        } catch (DateTimeParseException e) {
            throw new ErrorServiceException("La fecha de nacimiento no es válida.");
        }
    }

    public boolean perfilCompleto(String idUsuario) {
        Optional<Cliente> clienteOpt = buscarClientePorUsuario(idUsuario);
        if (clienteOpt.isEmpty()) {
            return false;
        }
        Cliente cliente = clienteOpt.get();
        return cliente.getNombre() != null && !cliente.getNombre().isBlank()
                && cliente.getApellido() != null && !cliente.getApellido().isBlank()
                && cliente.getSexo() != null
                && cliente.getFechaNacimiento() != null
                && cliente.getTipoDocumento() != null
                && cliente.getNumeroDocumento() != null && !cliente.getNumeroDocumento().isBlank()
                && cliente.getNacionalidad() != null
                && cliente.getDireccion() != null
                && cliente.getTelefono() != null;
    }

}
