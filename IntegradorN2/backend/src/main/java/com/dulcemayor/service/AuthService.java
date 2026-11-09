package com.dulcemayor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.CambioClaveRequest;
import com.dulcemayor.dto.LoginRequest;
import com.dulcemayor.dto.LoginResponse;
import com.dulcemayor.dto.UsuarioSesionResponse;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.security.JwtService;

/**
 * Casos de uso de la sesión para AuthController (E1-01): usa las reglas de UsuarioService,
 * emite el JWT y devuelve DTOs, así el controller nunca recibe la entidad Usuario.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthService(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        return respuesta(usuarioService.login(request.nombreUsuario(), request.clave()));
    }

    /** Cambia la clave y devuelve un token nuevo (los anteriores quedan invalidados). */
    @Transactional
    public LoginResponse modificarClave(String idUsuario, CambioClaveRequest request) {
        Usuario usuario = usuarioService.modificarClave(idUsuario, request.claveActual(), request.nuevaClave(),
                request.confirmarClave());
        return respuesta(usuario);
    }

    @Transactional
    public void cerrarSesion(String idUsuario) {
        usuarioService.cerrarSesion(idUsuario);
    }

    public UsuarioSesionResponse buscarSesion(String idUsuario) {
        Usuario usuario = usuarioService.buscarUsuario(idUsuario);
        return new UsuarioSesionResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getRol(),
                usuario.isClaveTemporal());
    }

    private LoginResponse respuesta(Usuario usuario) {
        return new LoginResponse(jwtService.emitir(usuario), usuario.getRol(), usuario.isClaveTemporal(),
                usuario.getNombreUsuario(), jwtService.getDuracionSegundos());
    }
}
