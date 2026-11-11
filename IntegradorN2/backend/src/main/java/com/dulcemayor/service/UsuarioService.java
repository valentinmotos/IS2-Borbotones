package com.dulcemayor.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.NoAutenticadoException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.utils.TextoUtils;

/**
 * Cuentas de usuario y credenciales (E1-01). Las reglas de clave y los mensajes vienen de
 * IntegradorN1 (UsuarioService); el login con hash ficticio y el cierre de sesión por versión
 * de token, de TPN2 Ej. 5d (UsuarioServicio).
 *
 * Devuelve la entidad Usuario porque solo lo consumen otros services (AuthService, el registro
 * de minoristas de E2-04 y el ABM de usuarios de E2-05), nunca un controller directamente.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    /** Clave con la que se crean las cuentas de empleados; se cambia en el primer ingreso. */
    public static final String CLAVE_POR_DEFECTO = "dulcemayor";
    public static final int LARGO_MINIMO_CLAVE = 8;
    /** BCrypt solo usa los primeros 72 bytes, que no equivalen a 72 caracteres Unicode. */
    public static final int LARGO_MAXIMO_CLAVE_BYTES = 72;
    public static final int LARGO_MAXIMO_CORREO = 254;

    private static final String CREDENCIALES_INCORRECTAS = "Correo o clave incorrectos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    /**
     * Si el correo no existe se compara igual contra este hash, así la respuesta tarda lo mismo
     * y no revela qué correos tienen cuenta (TPN2 Ej. 5d).
     */
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // ---- Alta ----

    /** Alta con la clave por defecto "dulcemayor" y cambio obligatorio en el primer ingreso. */
    @Transactional
    public Usuario crearUsuario(String nombreUsuario, RolUsuario rol) {
        return crearUsuario(nombreUsuario, rol, CLAVE_POR_DEFECTO, true);
    }

    /**
     * Alta con una clave elegida (registro del minorista en E2-04, usuarios de demo).
     * Con claveTemporal = true el usuario tiene que cambiarla en el primer ingreso.
     */
    @Transactional
    public Usuario crearUsuario(String nombreUsuario, RolUsuario rol, String clave, boolean claveTemporal) {
        validar(nombreUsuario, clave, rol);
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(normalizarCorreo(nombreUsuario));
        usuario.setRol(rol);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setClaveTemporal(claveTemporal);
        return usuarioRepository.save(usuario);
    }

    /** Crea el usuario y las credenciales pendientes de activación del registro público. */
    @Transactional
    public Usuario crearUsuarioPendienteMinorista(String correo, String clave, String codigo) {
        validar(correo, clave, RolUsuario.MINORISTA);
        validarCodigo(codigo);
        Instant ahora = Instant.now();
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(normalizarCorreo(correo));
        usuario.setRol(RolUsuario.MINORISTA);
        usuario.setClave(passwordEncoder.encode(clave));
        usuario.setClaveTemporal(false);
        usuario.setActivo(false);
        usuario.setCodigoActivacionHash(passwordEncoder.encode(codigo));
        usuario.setCodigoActivacionExpira(ahora.plus(Duration.ofMinutes(15)));
        usuario.setCodigoActivacionEnviado(ahora);
        usuario.setIntentosCodigoActivacion(0);
        return usuarioRepository.save(usuario);
    }

    /** Activa una cuenta solo si el código es correcto, no venció y aún conserva intentos. */
    @Transactional(noRollbackFor = ErrorServiceException.class)
    public Usuario activarCuentaMinorista(String correo, String codigo) {
        Usuario usuario = usuarioPendiente(correo);
        Instant ahora = Instant.now();
        if (usuario.getCodigoActivacionHash() == null || usuario.getCodigoActivacionExpira() == null
                || !ahora.isBefore(usuario.getCodigoActivacionExpira())
                || usuario.getIntentosCodigoActivacion() >= 5) {
            limpiarCodigo(usuario);
            usuarioRepository.save(usuario);
            throw new ErrorServiceException("El código no es válido o venció.", "codigo");
        }
        if (codigo == null || !codigo.matches("\\d{6}")
                || !passwordEncoder.matches(codigo, usuario.getCodigoActivacionHash())) {
            usuario.setIntentosCodigoActivacion(usuario.getIntentosCodigoActivacion() + 1);
            if (usuario.getIntentosCodigoActivacion() >= 5) limpiarCodigo(usuario);
            usuarioRepository.save(usuario);
            throw new ErrorServiceException("El código no es válido o venció.", "codigo");
        }
        usuario.setActivo(true);
        limpiarCodigo(usuario);
        return usuarioRepository.save(usuario);
    }

    /** Actualiza el hash y vencimiento para reenviar el código a una cuenta pendiente. */
    @Transactional
    public boolean prepararReenvioCodigoMinorista(String correo, String codigo) {
        if (correo == null || correo.isBlank()) return false;
        Usuario usuario = usuarioRepository.findByNombreUsuarioAndEliminadoFalse(normalizarCorreo(correo))
                .orElse(null);
        if (usuario == null || usuario.getRol() != RolUsuario.MINORISTA || usuario.isActivo()) return false;
        Instant ahora = Instant.now();
        if (usuario.getCodigoActivacionEnviado() != null
                && usuario.getCodigoActivacionEnviado().plusSeconds(60).isAfter(ahora)) return false;
        validarCodigo(codigo);
        usuario.setCodigoActivacionHash(passwordEncoder.encode(codigo));
        usuario.setCodigoActivacionExpira(ahora.plus(Duration.ofMinutes(15)));
        usuario.setCodigoActivacionEnviado(ahora);
        usuario.setIntentosCodigoActivacion(0);
        usuarioRepository.save(usuario);
        return true;
    }

    private Usuario usuarioPendiente(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ErrorServiceException("El correo electrónico es obligatorio.", "correo");
        }
        Usuario usuario = usuarioRepository.findByNombreUsuarioAndEliminadoFalse(normalizarCorreo(correo))
                .orElseThrow(() -> new ErrorServiceException("El código no es válido o venció.", "codigo"));
        if (usuario.getRol() != RolUsuario.MINORISTA || usuario.isActivo()) {
            throw new ErrorServiceException("El código no es válido o venció.", "codigo");
        }
        return usuario;
    }

    private void limpiarCodigo(Usuario usuario) {
        usuario.setCodigoActivacionHash(null);
        usuario.setCodigoActivacionExpira(null);
        usuario.setCodigoActivacionEnviado(null);
        usuario.setIntentosCodigoActivacion(0);
    }

    private void validarCodigo(String codigo) {
        if (codigo == null || !codigo.matches("\\d{6}")) {
            throw new ErrorServiceException("El código de activación debe tener seis dígitos.", "codigo");
        }
    }

    /** Valida correo y rol del ABM de usuarios internos sin aceptar cuentas de minoristas. */
    public void validarUsuarioInterno(String correo, RolUsuario rol, String idActual) {
        validarCorreo(correo);
        if (rol == null || rol == RolUsuario.MINORISTA) {
            throw new ErrorServiceException("Elegí un rol interno válido.", "rol");
        }
        String normalizado = normalizarCorreo(correo);
        usuarioRepository.findByNombreUsuario(normalizado).ifPresent(existente -> {
            if (idActual == null || !existente.getId().equals(idActual)) {
                throw new DuplicadoException("Ya existe una cuenta registrada con ese correo.", "correo");
            }
        });
    }

    /** Crea o resetea credenciales de personal con la clave temporal del proyecto. */
    void establecerClaveTemporal(Usuario usuario) {
        usuario.setClave(passwordEncoder.encode(CLAVE_POR_DEFECTO));
        usuario.setClaveTemporal(true);
        usuario.setActivo(true);
    }

    @Transactional
    public Usuario resetearClaveTemporal(String id) {
        Usuario usuario = buscarUsuario(id);
        establecerClaveTemporal(usuario);
        usuario.setVersionToken(usuario.getVersionToken() + 1);
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarUsuariosInternos() {
        return usuarioRepository.findByEliminadoFalseAndRolInOrderByNombreUsuarioAsc(
                List.of(RolUsuario.JEFE, RolUsuario.ADMINISTRATIVO, RolUsuario.PREVENTOR, RolUsuario.DISTRIBUIDOR));
    }

    public long contarJefesActivos() {
        return usuarioRepository.countByRolAndActivoTrueAndEliminadoFalse(RolUsuario.JEFE);
    }

    /** validar(nombreUsuario, clave, rol) del UML: reglas del alta. */
    public void validar(String nombreUsuario, String clave, RolUsuario rol) {
        validarCorreo(nombreUsuario);
        if (usuarioRepository.existsByNombreUsuario(normalizarCorreo(nombreUsuario))) {
            throw new DuplicadoException("Ya existe una cuenta registrada con ese correo.", "nombreUsuario");
        }
        if (rol == null) {
            throw new ErrorServiceException("El rol es obligatorio.", "rol");
        }
        validarClave(clave, "clave");
    }

    // ---- Ingreso y sesión ----

    /**
     * Comprueba correo y clave. Cualquier falla responde el mismo mensaje para no revelar
     * si el correo existe. Una cuenta sin activar (registro del minorista, E2-04) no ingresa.
     */
    public Usuario login(String nombreUsuario, String clave) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null
                || excedeLargoMaximo(clave)) {
            throw new NoAutenticadoException(CREDENCIALES_INCORRECTAS);
        }
        Usuario usuario = usuarioRepository
                .findByNombreUsuarioAndEliminadoFalse(normalizarCorreo(nombreUsuario))
                .orElse(null);
        boolean coincide = passwordEncoder.matches(clave, usuario == null ? hashFicticio : usuario.getClave());
        if (usuario == null || !coincide) {
            throw new NoAutenticadoException(CREDENCIALES_INCORRECTAS);
        }
        if (!usuario.isActivo()) {
            throw new NoAutenticadoException(
                    "Tu cuenta todavía no está activada. Revisá tu correo para activarla.");
        }
        return usuario;
    }

    /** true mientras el usuario siga con una clave temporal (por ejemplo "dulcemayor"). */
    public boolean requiereCambioClave(String id) {
        return buscarUsuario(id).isClaveTemporal();
    }

    /**
     * Cambio de clave del usuario logueado (voluntario u obligatorio por clave temporal).
     * Exige la clave actual, aplica las reglas del alta y no permite repetir la misma clave.
     * Invalida los tokens anteriores: quien siga con uno viejo tiene que volver a ingresar.
     */
    @Transactional
    public Usuario modificarClave(String id, String claveActual, String nuevaClave, String confirmarClave) {
        if (claveActual == null || claveActual.isEmpty()) {
            throw new ErrorServiceException("Ingresá tu clave actual.", "claveActual");
        }
        Usuario usuario = buscarUsuario(id);
        if (!passwordEncoder.matches(claveActual, usuario.getClave())) {
            throw new ErrorServiceException("La clave actual no es correcta.", "claveActual");
        }
        validarClave(nuevaClave, "nuevaClave");
        if (!nuevaClave.equals(confirmarClave)) {
            throw new ErrorServiceException("La confirmación no coincide con la clave nueva.", "confirmarClave");
        }
        if (nuevaClave.equals(claveActual)) {
            throw new ErrorServiceException("La clave nueva tiene que ser distinta de la actual.", "nuevaClave");
        }
        usuario.setClave(passwordEncoder.encode(nuevaClave));
        usuario.setClaveTemporal(false);
        usuario.setVersionToken(usuario.getVersionToken() + 1);
        return usuarioRepository.save(usuario);
    }

    /** Cierre de sesión: todos los tokens emitidos hasta ahora dejan de valer. */
    @Transactional
    public void cerrarSesion(String id) {
        Usuario usuario = buscarUsuario(id);
        usuario.setVersionToken(usuario.getVersionToken() + 1);
        usuarioRepository.save(usuario);
    }

    // ---- Consultas ----

    public Usuario buscarUsuario(String id) {
        if (id == null || id.isBlank()) {
            throw new RecursoNoEncontradoException("El usuario no existe o fue dado de baja.");
        }
        return usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe o fue dado de baja."));
    }

    public Usuario buscarUsuarioPorNombre(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new RecursoNoEncontradoException("El usuario no existe o fue dado de baja.");
        }
        return usuarioRepository.findByNombreUsuarioAndEliminadoFalse(normalizarCorreo(nombreUsuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe o fue dado de baja."));
    }

    // ---- Reglas (de IntegradorN1) ----

    private void validarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ErrorServiceException("El correo electrónico es obligatorio.", "nombreUsuario");
        }
        if (correo.strip().length() > LARGO_MAXIMO_CORREO) {
            throw new ErrorServiceException("El correo electrónico no puede superar los " + LARGO_MAXIMO_CORREO
                    + " caracteres.", "nombreUsuario");
        }
        if (!TextoUtils.esCorreoValido(correo)) {
            throw new ErrorServiceException("El correo electrónico no tiene un formato válido.", "nombreUsuario");
        }
    }

    private void validarClave(String clave, String campo) {
        if (clave == null || clave.isEmpty()) {
            throw new ErrorServiceException("La clave es obligatoria.", campo);
        }
        if (clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ErrorServiceException("La clave tiene que tener al menos " + LARGO_MINIMO_CLAVE
                    + " caracteres.", campo);
        }
        if (excedeLargoMaximo(clave)) {
            throw new ErrorServiceException("La clave es demasiado larga.", campo);
        }
    }

    private static boolean excedeLargoMaximo(String clave) {
        return clave.getBytes(StandardCharsets.UTF_8).length > LARGO_MAXIMO_CLAVE_BYTES;
    }

    public static String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }
}
