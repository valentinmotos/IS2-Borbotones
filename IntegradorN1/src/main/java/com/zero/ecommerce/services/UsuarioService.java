package com.zero.ecommerce.services;

import java.security.SecureRandom;
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
        Optional<Persona> persona = personaRepository
                .findFirstByUsuario_NombreUsuarioIgnoreCaseAndEliminadoFalse(usuario.getNombreUsuario());
        if (persona.isEmpty()) {
            return usuario.getNombreUsuario();
        }

        String nombre = persona.get().getNombre() == null ? "" : persona.get().getNombre().strip();
        String apellido = persona.get().getApellido() == null ? "" : persona.get().getApellido().strip();
        String nombreCompleto = (nombre + " " + apellido).strip();
        return nombreCompleto.isBlank() ? usuario.getNombreUsuario() : nombreCompleto;
    }
}
