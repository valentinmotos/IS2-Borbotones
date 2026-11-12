package com.dulcemayor.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.LoginResponse;
import com.dulcemayor.dto.RegistroMinoristaRequest;
import com.dulcemayor.entity.IdentidadExterna;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.NoAutenticadoException;
import com.dulcemayor.repository.IdentidadExternaRepository;
import com.dulcemayor.repository.MinoristaRepository;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.security.GoogleOAuthFlow;
import com.dulcemayor.security.GoogleOAuthFlow.Identidad;

@Service
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class GoogleOAuthService {

    private final IdentidadExternaRepository identidades;
    private final UsuarioRepository usuarios;
    private final MinoristaRepository minoristas;
    private final UsuarioService usuarioService;
    private final RegistroMinoristaService registroService;
    private final AuthService authService;

    public GoogleOAuthService(IdentidadExternaRepository identidades, UsuarioRepository usuarios,
            MinoristaRepository minoristas, UsuarioService usuarioService, RegistroMinoristaService registroService,
            AuthService authService) {
        this.identidades = identidades;
        this.usuarios = usuarios;
        this.minoristas = minoristas;
        this.usuarioService = usuarioService;
        this.registroService = registroService;
        this.authService = authService;
    }

    public enum Estado {
        SIN_FLUJO, REGISTRO, EXISTENTE, PENDIENTE, VINCULADA, BLOQUEADA
    }

    public Estado estado(Identidad identidad) {
        if (identidad == null) {
            return Estado.SIN_FLUJO;
        }
        return identidades.findByEmisorAndSujeto(GoogleOAuthFlow.EMISOR, identidad.sujeto())
                .map(vinculo -> {
                    Usuario usuario = vinculo.getUsuario();
                    if (!esMinoristaHabilitable(usuario)) {
                        return Estado.BLOQUEADA;
                    }
                    if (usuario.isActivo()) {
                        return Estado.VINCULADA;
                    }
                    return usuario.getNombreUsuario().equals(identidad.correo()) ? Estado.PENDIENTE : Estado.BLOQUEADA;
                })
                .orElseGet(() -> usuarios.existsByNombreUsuario(identidad.correo()) ? Estado.EXISTENTE : Estado.REGISTRO);
    }

    public boolean tieneVinculo(String usuarioId) {
        return usuarioId != null && identidades.existsByUsuario_IdAndEmisor(usuarioId, GoogleOAuthFlow.EMISOR);
    }

    public LoginResponse iniciarSesion(Identidad identidad) {
        Usuario usuario = identidades.findByEmisorAndSujeto(GoogleOAuthFlow.EMISOR, identidad.sujeto())
                .map(IdentidadExterna::getUsuario).orElseThrow(GoogleOAuthService::sesionNoHabilitada);
        if (!esMinoristaHabilitable(usuario) || !usuario.isActivo()) {
            throw sesionNoHabilitada();
        }
        return authService.respuesta(usuario);
    }

    @Transactional
    public String registrar(Identidad identidad, RegistroMinoristaRequest datos) {
        if (!identidad.correo().equalsIgnoreCase(datos.correo().strip())) {
            throw new ErrorServiceException("Usá el correo que verificaste con Google.", "correo");
        }
        if (estado(identidad) != Estado.REGISTRO) {
            throw new DuplicadoException("Ya existe una cuenta para este acceso. Ingresá desde el portal.", "correo");
        }
        MinoristaService.validarEdad(datos.fechaNacimiento());
        try {
            String mensaje = registroService.registrar(datos);
            Usuario usuario = usuarioService.buscarUsuarioPorNombre(identidad.correo());
            identidades.saveAndFlush(new IdentidadExterna(GoogleOAuthFlow.EMISOR, identidad.sujeto(), usuario));
            return mensaje;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicadoException("Ya hay una cuenta con ese correo, documento o identidad de Google.", null);
        }
    }

    @Transactional
    public void vincular(String usuarioId, String clave, Identidad identidad) {
        Usuario usuario = confirmarClave(usuarioId, clave);
        try {
            identidades.saveAndFlush(new IdentidadExterna(GoogleOAuthFlow.EMISOR, identidad.sujeto(), usuario));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicadoException("La cuenta de Google o la cuenta minorista ya tienen un vínculo.", null);
        }
    }

    @Transactional
    public LoginResponse desvincular(String usuarioId, String clave) {
        Usuario usuario = confirmarClave(usuarioId, clave);
        IdentidadExterna identidad = identidades.findByUsuario_IdAndEmisor(usuarioId, GoogleOAuthFlow.EMISOR)
                .orElseThrow(() -> new ErrorServiceException("Tu cuenta no tiene un vínculo con Google."));
        identidades.delete(identidad);
        usuarioService.cerrarSesion(usuarioId);
        return authService.respuesta(usuario);
    }

    private Usuario confirmarClave(String usuarioId, String clave) {
        Usuario usuario = usuarioService.buscarUsuario(usuarioId);
        if (!usuario.isActivo() || !esMinoristaHabilitable(usuario)) {
            throw accesoDenegado();
        }
        try {
            return usuarioService.login(usuario.getNombreUsuario(), clave);
        } catch (NoAutenticadoException ex) {
            throw new ErrorServiceException("La clave actual no es correcta.", "clave");
        }
    }

    private boolean esMinoristaHabilitable(Usuario usuario) {
        return !usuario.isEliminado() && !usuario.isClaveTemporal() && usuario.getRol() == RolUsuario.MINORISTA
                && minoristas.findByUsuario_IdAndEliminadoFalse(usuario.getId()).isPresent();
    }

    private static AccessDeniedException accesoDenegado() {
        return new AccessDeniedException("La cuenta no está habilitada para el acceso con Google.");
    }

    private static NoAutenticadoException sesionNoHabilitada() {
        return new NoAutenticadoException("El acceso con Google no está habilitado para esta cuenta.");
    }
}
