package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Empleado;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.entities.enums.TipoDocumento;
import com.zero.ecommerce.entities.enums.TipoEmpleado;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.EmpleadoRepository;
import com.zero.ecommerce.repositories.PersonaRepository;

/** ABM de empleados; su cuenta se crea y administra en conjunto con UsuarioService. */
@Service
@Transactional(readOnly = true)
public class EmpleadoService {
    private static final int LARGO_MAXIMO_NOMBRE = 100;
    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 120;
    private static final Pattern DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern PASAPORTE = Pattern.compile("[A-Z0-9]{6,15}");

    private final EmpleadoRepository empleadoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioService usuarioService;

    public EmpleadoService(EmpleadoRepository empleadoRepository, PersonaRepository personaRepository,
            UsuarioService usuarioService) {
        this.empleadoRepository = empleadoRepository;
        this.personaRepository = personaRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public Empleado crearEmpleado(String nombre, String apellido, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, String correo, RolUsuario rol, String clave,
            String confirmacionClave) throws ErrorServiceException {
        validar(nombre, apellido, fechaNacimiento, tipoDocumento, numeroDocumento, null);
        Usuario usuario = usuarioService.crearUsuarioEmpleado(correo, rol, clave, confirmacionClave);
        Empleado empleado = new Empleado();
        asignarDatos(empleado, nombre, apellido, fechaNacimiento, tipoDocumento, numeroDocumento, rol);
        asociarEmpleadoUsuario(empleado, usuario);
        return empleado;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarEmpleado(String id, String nombre, String apellido, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, String correo, RolUsuario rol, String clave,
            String confirmacionClave) throws ErrorServiceException {
        Empleado empleado = buscarEmpleado(id);
        validar(nombre, apellido, fechaNacimiento, tipoDocumento, numeroDocumento, id);
        usuarioService.modificarUsuarioEmpleado(empleado.getUsuario().getId(), correo, rol, clave, confirmacionClave);
        asignarDatos(empleado, nombre, apellido, fechaNacimiento, tipoDocumento, numeroDocumento, rol);
        empleadoRepository.save(empleado);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarEmpleadoPorUsuario(String idUsuario, String idUsuarioActual) throws ErrorServiceException {
        Empleado empleado = empleadoRepository.findByUsuario_IdAndEliminadoFalse(idUsuario)
                .orElseThrow(() -> new ErrorServiceException("El empleado no existe o fue eliminado."));
        usuarioService.eliminarUsuario(idUsuario, idUsuarioActual);
        empleado.setEliminado(true);
        empleadoRepository.save(empleado);
    }

    /** Método del diagrama: persiste la asociación y evita empleados sin cuenta. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void asociarEmpleadoUsuario(Empleado empleado, Usuario usuario) throws ErrorServiceException {
        if (empleado == null || usuario == null) {
            throw new ErrorServiceException("El empleado y el usuario son obligatorios.");
        }
        empleado.setUsuario(usuario);
        empleadoRepository.save(empleado);
    }

    public Empleado buscarEmpleado(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) throw new ErrorServiceException("El empleado no existe o fue eliminado.");
        return empleadoRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El empleado no existe o fue eliminado."));
    }

    public Empleado buscarEmpleadoPorUsuario(String idUsuario) throws ErrorServiceException {
        if (idUsuario == null || idUsuario.isBlank()) throw new ErrorServiceException("El empleado no existe o fue eliminado.");
        return empleadoRepository.findByUsuario_IdAndEliminadoFalse(idUsuario)
                .orElseThrow(() -> new ErrorServiceException("El empleado no existe o fue eliminado."));
    }

    public List<Empleado> listarEmpleado() {
        return empleadoRepository.findAll();
    }

    public List<Empleado> listarEmpleadoActivo() {
        return empleadoRepository.findByEliminadoFalseOrderByApellidoAscNombreAsc();
    }

    /** Baja por identificador de empleado; se conserva la variante por usuario para el listado de cuentas. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarEmpleado(String id, String idUsuarioActual) throws ErrorServiceException {
        Empleado empleado = buscarEmpleado(id);
        eliminarEmpleadoPorUsuario(empleado.getUsuario().getId(), idUsuarioActual);
    }

    public void validar(String nombre, String apellido, LocalDate fechaNacimiento, TipoDocumento tipoDocumento,
            String numeroDocumento, String idActual) throws ErrorServiceException {
        validarTexto(nombre, "El nombre");
        validarTexto(apellido, "El apellido");
        validarFechaNacimiento(fechaNacimiento);
        validarDocumento(tipoDocumento, numeroDocumento, idActual);
    }

    public TipoDocumento convertirTipoDocumento(String tipo) throws ErrorServiceException {
        if (tipo == null || tipo.isBlank()) return null;
        try {
            return TipoDocumento.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            throw new ErrorServiceException("El tipo de documento no es válido.");
        }
    }

    public LocalDate convertirFechaNacimiento(String fecha) throws ErrorServiceException {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDate.parse(fecha.strip());
        } catch (DateTimeParseException e) {
            throw new ErrorServiceException("La fecha de nacimiento no es válida.");
        }
    }

    public Map<String, String> listarTiposDocumento() {
        Map<String, String> tipos = new LinkedHashMap<>();
        tipos.put(TipoDocumento.DNI.name(), "DNI");
        tipos.put(TipoDocumento.PASAPORTE.name(), "Pasaporte");
        return tipos;
    }

    private void validarTexto(String valor, String campo) throws ErrorServiceException {
        if (valor == null || valor.isBlank()) throw new ErrorServiceException(campo + " es obligatorio.");
        if (valor.strip().length() > LARGO_MAXIMO_NOMBRE) {
            throw new ErrorServiceException(campo + " no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) throws ErrorServiceException {
        if (fechaNacimiento == null) throw new ErrorServiceException("La fecha de nacimiento es obligatoria.");
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (fechaNacimiento.isAfter(LocalDate.now()) || edad > EDAD_MAXIMA) {
            throw new ErrorServiceException("La fecha de nacimiento no es válida.");
        }
        if (edad < EDAD_MINIMA) throw new ErrorServiceException("El empleado tiene que ser mayor de edad.");
    }

    private void validarDocumento(TipoDocumento tipoDocumento, String numeroDocumento, String idActual)
            throws ErrorServiceException {
        if (tipoDocumento == null) throw new ErrorServiceException("El tipo de documento es obligatorio.");
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
        boolean repetido = personaRepository.findByTipoDocumentoAndNumeroDocumentoAndEliminadoFalse(tipoDocumento, numero)
                .stream().anyMatch(persona -> !persona.getId().equals(idActual));
        if (repetido) throw new ErrorServiceException("Ya hay otra persona registrada con ese documento.");
    }

    private void asignarDatos(Empleado empleado, String nombre, String apellido, LocalDate fechaNacimiento,
            TipoDocumento tipoDocumento, String numeroDocumento, RolUsuario rol) {
        empleado.setNombre(nombre.strip());
        empleado.setApellido(apellido.strip());
        empleado.setFechaNacimiento(fechaNacimiento);
        empleado.setTipoDocumento(tipoDocumento);
        empleado.setNumeroDocumento(normalizarDocumento(numeroDocumento));
        empleado.setTipoEmpleado(rol == RolUsuario.JEFE ? TipoEmpleado.JEFE : TipoEmpleado.ADMINISTRATIVO);
    }

    private String normalizarDocumento(String numeroDocumento) {
        return numeroDocumento.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }
}
