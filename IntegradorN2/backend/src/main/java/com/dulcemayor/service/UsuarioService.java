package com.dulcemayor.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

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

    private static String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }
}
