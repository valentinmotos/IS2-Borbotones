package com.zero.ecommerce.services;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.UsuarioAdminDTO;
import com.zero.ecommerce.entities.Persona;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.PersonaRepository;
import com.zero.ecommerce.repositories.UsuarioRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    // Reglas de clave del registro (RF01). E2-06 y E2-08 aplican las mismas.
    public static final int LARGO_MINIMO_CLAVE = 8;
    private static final int LARGO_MAXIMO_CLAVE = 64;
    private static final int LARGO_MAXIMO_CORREO = 150;
    private static final String ASUNTO_ACTIVACION = "Activá tu cuenta de Zero";

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public UsuarioService(UsuarioRepository usuarioRepository, PersonaRepository personaRepository,
            PasswordEncoder passwordEncoder, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registro de un cliente desde el sitio (RF01). Crea el usuario con rol CLIENTE, la clave encriptada
     * y un código de activación de 6 dígitos: hasta activarla, la cuenta no puede iniciar sesión (RF02).
     * El correo con el código lo manda enviarCodigoActivacion.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Usuario registrarCliente(String correo, String clave, String confirmacion) throws ErrorServiceException {
        validarRegistro(correo, clave, confirmacion);
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(normalizarCorreo(correo));
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setCodigoActivacion(generarCodigoActivacion());
        return usuarioRepository.save(usuario);
    }

    public void validarRegistro(String correo, String clave, String confirmacion) throws ErrorServiceException {
        validarCorreo(correo);
        Optional<Usuario> existente = usuarioRepository.findByNombreUsuarioIgnoreCase(correo.strip());
        if (existente.isPresent()) {
            throw new ErrorServiceException(existente.get().getCodigoActivacion() != null
                    ? "Ya hay una cuenta registrada con ese correo que todavía no se activó. Ingresá el código que te enviamos o pedí uno nuevo."
                    : "Ya hay una cuenta registrada con ese correo.");
        }
        validarClave(clave, confirmacion);
    }

    /**
     * Activa la cuenta si el código coincide con el guardado: lo pone en null y desde ese momento
     * el usuario puede iniciar sesión.
     */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void activarCuenta(String correo, String codigo) throws ErrorServiceException {
        if (correo == null || correo.isBlank()) {
            throw new ErrorServiceException("El correo electrónico es obligatorio.");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new ErrorServiceException("El código de activación es obligatorio.");
        }
        Usuario usuario = buscarUsuarioParaActivar(correo);
        if (!usuario.getCodigoActivacion().equals(codigo.strip())) {
            throw new ErrorServiceException("El código de activación no es correcto.");
        }
        usuario.setCodigoActivacion(null);
        usuarioRepository.save(usuario);
    }

    /** Genera un código nuevo para una cuenta pendiente de activación. El anterior deja de servir. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Usuario reenviarCodigoActivacion(String correo) throws ErrorServiceException {
        if (correo == null || correo.isBlank()) {
            throw new ErrorServiceException("Ingresá el correo con el que te registraste.");
        }
        Usuario usuario = buscarUsuarioParaActivar(correo);
        usuario.setCodigoActivacion(generarCodigoActivacion());
        return usuarioRepository.save(usuario);
    }

    /**
     * Manda el correo con el código (email/activacion.html). urlActivacion es la URL absoluta de
     * /registro/activar con el correo: la arma el controller, porque el envío corre sin request.
     * Es asincrónico: si el correo no está configurado, el error queda en el log.
     */
    public void enviarCodigoActivacion(Usuario usuario, String urlActivacion) {
        // Para poder activar cuentas en desarrollo sin configurar el correo de la empresa.
        log.info("Código de activación de {}: {}", usuario.getNombreUsuario(), usuario.getCodigoActivacion());
        emailService.enviar(usuario.getNombreUsuario(), ASUNTO_ACTIVACION, "activacion",
                Map.of("correo", usuario.getNombreUsuario(), "codigo", usuario.getCodigoActivacion(),
                        "urlActivacion", urlActivacion));
    }

    // Busca una cuenta pendiente de activación, con un mensaje distinto si ya está activa.
    private Usuario buscarUsuarioParaActivar(String correo) throws ErrorServiceException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCaseAndEliminadoFalse(correo.strip())
                .orElseThrow(() -> new ErrorServiceException("No hay una cuenta registrada con el correo "
                        + correo.strip() + "."));
        if (usuario.getCodigoActivacion() == null) {
            throw new ErrorServiceException("La cuenta ya está activada. Ya podés ingresar.");
        }
        return usuario;
    }

    private void validarCorreo(String correo) throws ErrorServiceException {
        if (correo == null || correo.isBlank()) {
            throw new ErrorServiceException("El correo electrónico es obligatorio.");
        }
        if (correo.strip().length() > LARGO_MAXIMO_CORREO) {
            throw new ErrorServiceException("El correo electrónico no puede superar los " + LARGO_MAXIMO_CORREO
                    + " caracteres.");
        }
        if (!TextoUtils.esCorreoValido(correo)) {
            throw new ErrorServiceException("El correo electrónico no tiene un formato válido.");
        }
    }

    private void validarClave(String clave, String confirmacion) throws ErrorServiceException {
        if (clave == null || clave.isEmpty()) {
            throw new ErrorServiceException("La clave es obligatoria.");
        }
        if (clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ErrorServiceException("La clave tiene que tener al menos " + LARGO_MINIMO_CLAVE
                    + " caracteres.");
        }
        if (clave.length() > LARGO_MAXIMO_CLAVE) {
            throw new ErrorServiceException("La clave no puede superar los " + LARGO_MAXIMO_CLAVE + " caracteres.");
        }
        if (!clave.equals(confirmacion)) {
            throw new ErrorServiceException("La confirmación no coincide con la clave.");
        }
    }

    private String generarCodigoActivacion() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    public Usuario buscarUsuario(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El usuario no existe o fue eliminado.");
        }
        return usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("El usuario no existe o fue eliminado."));
    }

    public Usuario buscarUsuarioPorNombre(String nombreUsuario) throws ErrorServiceException {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new ErrorServiceException("El usuario no existe o fue eliminado.");
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCaseAndEliminadoFalse(nombreUsuario.strip())
                .orElseThrow(() -> new ErrorServiceException("El usuario no existe o fue eliminado."));
    }

    /** Crea una cuenta de empleado activa. Las cuentas CLIENTE solo se crean desde el registro público. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Usuario crearUsuarioEmpleado(String correo, RolUsuario rol, String clave, String confirmacion)
            throws ErrorServiceException {
        validarUsuarioEmpleado(correo, rol, clave, confirmacion, null, true);
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(normalizarCorreo(correo));
        usuario.setRol(rol);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setCodigoActivacion(null);
        return usuarioRepository.save(usuario);
    }

    /** Actualiza correo, rol y, solo si se recibe, una nueva clave del empleado. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarUsuarioEmpleado(String id, String correo, RolUsuario rol, String clave, String confirmacion)
            throws ErrorServiceException {
        Usuario usuario = buscarUsuario(id);
        if (usuario.getRol() == RolUsuario.CLIENTE) {
            throw new ErrorServiceException("Los datos de un cliente se editan desde su perfil.");
        }
        validarUsuarioEmpleado(correo, rol, clave, confirmacion, id, false);
        validarQueNoSeaUltimoJefe(usuario, rol);
        usuario.setNombreUsuario(normalizarCorreo(correo));
        usuario.setRol(rol);
        if (clave != null && !clave.isBlank()) {
            usuario.setClave(passwordEncoder.encode(clave));
        }
        usuarioRepository.save(usuario);
    }

    /** Baja lógica de una cuenta. Protege la sesión actual y la existencia de al menos un JEFE activo. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarUsuario(String id, String idUsuarioActual) throws ErrorServiceException {
        Usuario usuario = buscarUsuario(id);
        if (usuario.getId().equals(idUsuarioActual)) {
            throw new ErrorServiceException("No podés darte de baja a vos mismo.");
        }
        validarQueNoSeaUltimoJefe(usuario, null);
        usuario.setEliminado(true);
        usuarioRepository.save(usuario);
    }

    /** Los clientes no los edita el panel: únicamente se vuelve a habilitar su cuenta. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void reactivarCliente(String id) throws ErrorServiceException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ErrorServiceException("El usuario no existe."));
        if (usuario.getRol() != RolUsuario.CLIENTE) {
            throw new ErrorServiceException("Solo se pueden reactivar cuentas de clientes desde este listado.");
        }
        if (!usuario.isEliminado()) {
            throw new ErrorServiceException("La cuenta del cliente ya está activa.");
        }
        usuario.setEliminado(false);
        usuarioRepository.save(usuario);
    }

    /** Listado administrativo, con estado derivado de baja lógica y activación de cuenta. */
    public List<UsuarioAdminDTO> listarUsuarios(String rolFiltro, String estadoFiltro, String buscar) {
        String texto = buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
        return usuarioRepository.findAllByOrderByNombreUsuarioAsc().stream()
                .filter(usuario -> rolFiltro == null || rolFiltro.isBlank() || usuario.getRol().name().equals(rolFiltro))
                .filter(usuario -> coincideEstado(usuario, estadoFiltro))
                .filter(usuario -> texto.isBlank() || usuario.getNombreUsuario().toLowerCase(Locale.ROOT).contains(texto))
                .map(usuario -> new UsuarioAdminDTO(usuario.getId(), nombreParaMostrar(usuario), usuario.getNombreUsuario(),
                        usuario.getRol().name(), describirEstado(usuario), !usuario.isEliminado(),
                        usuario.getRol() != RolUsuario.CLIENTE, usuario.getRol() == RolUsuario.CLIENTE))
                .toList();
    }

    /** Consulta administrativa: incluye cuentas dadas de baja para poder auditarlas o reactivarlas. */
    public Usuario buscarUsuarioAdministracion(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) throw new ErrorServiceException("El usuario no existe.");
        return usuarioRepository.findById(id).orElseThrow(() -> new ErrorServiceException("El usuario no existe."));
    }

    public List<Usuario> listarUsuario() {
        return usuarioRepository.findAllByOrderByNombreUsuarioAsc();
    }

    public List<Usuario> listarUsuarioActivo() {
        return listarUsuario().stream().filter(usuario -> !usuario.isEliminado()).toList();
    }

    public RolUsuario convertirRolEmpleado(String rol) throws ErrorServiceException {
        if (rol == null || rol.isBlank()) {
            throw new ErrorServiceException("El rol es obligatorio.");
        }
        try {
            RolUsuario valor = RolUsuario.valueOf(rol);
            if (valor == RolUsuario.CLIENTE) throw new ErrorServiceException("Un empleado no puede tener rol CLIENTE.");
            return valor;
        } catch (IllegalArgumentException e) {
            throw new ErrorServiceException("El rol seleccionado no es válido.");
        }
    }

    public Map<String, String> listarRolesEmpleado() {
        Map<String, String> roles = new LinkedHashMap<>();
        roles.put(RolUsuario.JEFE.name(), "Jefe");
        roles.put(RolUsuario.ADMINISTRATIVO.name(), "Administrativo");
        return roles;
    }

    private void validarUsuarioEmpleado(String correo, RolUsuario rol, String clave, String confirmacion,
            String idActual, boolean claveObligatoria) throws ErrorServiceException {
        validarCorreo(correo);
        Usuario existente = usuarioRepository.findByNombreUsuarioIgnoreCase(normalizarCorreo(correo)).orElse(null);
        if (existente != null && !existente.getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe una cuenta registrada con ese correo.");
        }
        if (rol == null || rol == RolUsuario.CLIENTE) {
            throw new ErrorServiceException("El rol del empleado debe ser JEFE o ADMINISTRATIVO.");
        }
        boolean cambioClave = clave != null && !clave.isBlank();
        if (claveObligatoria && !cambioClave) {
            throw new ErrorServiceException("La clave es obligatoria.");
        }
        if (!cambioClave && confirmacion != null && !confirmacion.isBlank()) {
            throw new ErrorServiceException("Ingresá una clave nueva para confirmar el cambio.");
        }
        if (cambioClave) validarClave(clave, confirmacion);
    }

    private void validarQueNoSeaUltimoJefe(Usuario usuario, RolUsuario nuevoRol) throws ErrorServiceException {
        boolean dejaDeSerJefe = usuario.getRol() == RolUsuario.JEFE
                && (nuevoRol == null || nuevoRol != RolUsuario.JEFE);
        if (dejaDeSerJefe && usuarioRepository.countByRolAndEliminadoFalse(RolUsuario.JEFE) <= 1) {
            throw new ErrorServiceException("Debe quedar al menos un JEFE activo en el sistema.");
        }
    }

    private boolean coincideEstado(Usuario usuario, String estado) {
        if (estado == null || estado.isBlank() || "TODOS".equals(estado)) return true;
        return switch (estado) {
            case "ACTIVO" -> !usuario.isEliminado() && usuario.getCodigoActivacion() == null;
            case "PENDIENTE" -> !usuario.isEliminado() && usuario.getCodigoActivacion() != null;
            case "INACTIVO" -> usuario.isEliminado();
            default -> false;
        };
    }

    private String describirEstado(Usuario usuario) {
        if (usuario.isEliminado()) return "Inactivo";
        return usuario.getCodigoActivacion() == null ? "Activo" : "Pendiente de activación";
    }

    public Optional<Usuario> usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByNombreUsuarioIgnoreCaseAndEliminadoFalse(authentication.getName());
    }

    public String nombreParaMostrar(Usuario usuario) {
        if (usuario == null) {
            return "";
        }
        // Si la cuenta está dada de baja, su persona también: se usa igual para mostrar el nombre en el ABM.
        Optional<Persona> persona = personaRepository
                .findFirstByUsuario_NombreUsuarioIgnoreCaseAndEliminadoFalse(usuario.getNombreUsuario())
                .or(() -> usuario.isEliminado() ? personaRepository.findFirstByUsuario_Id(usuario.getId())
                        : Optional.empty());
        if (persona.isEmpty()) {
            return usuario.getNombreUsuario();
        }

        String nombre = persona.get().getNombre() == null ? "" : persona.get().getNombre().strip();
        String apellido = persona.get().getApellido() == null ? "" : persona.get().getApellido().strip();
        String nombreCompleto = (nombre + " " + apellido).strip();
        return nombreCompleto.isBlank() ? usuario.getNombreUsuario() : nombreCompleto;
    }

    /** Cambio de clave del usuario logueado: exige la clave actual y aplica las reglas del registro. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarClave(String id, String claveActual, String nuevaClave, String confirmarClave)
            throws ErrorServiceException {
        if (claveActual == null || claveActual.isEmpty()) {
            throw new ErrorServiceException("Ingresá tu clave actual.");
        }
        Usuario usuario = buscarUsuario(id);
        if (!passwordEncoder.matches(claveActual, usuario.getClave())) {
            throw new ErrorServiceException("La clave actual no es correcta.");
        }
        validarClave(nuevaClave, confirmarClave);
        usuario.setClave(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);
    }

}
