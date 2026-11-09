package com.dulcemayor.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.repository.UsuarioRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee "Authorization: Bearer <token>" y, si el token es válido, autentica la petición (E1-01,
 * adaptado de TPN2 Ej. 5d). Además de la firma y el vencimiento comprueba en la base que el
 * usuario siga existiendo, no esté dado de baja, esté activo y que el token no sea de una sesión
 * cerrada (versión distinta). Si algo falla la petición sigue sin autenticar y termina en 401.
 *
 * Mientras la clave sea temporal no recibe su rol sino solo la autoridad CLAVE_TEMPORAL, así
 * no puede usar ningún endpoint salvo los de /api/auth (cambio de clave, sesión y logout).
 *
 * No es un @Component: lo crea SecurityConfig para que se registre solo en la cadena de seguridad.
 */
public class JwtFiltro extends OncePerRequestFilter {

    public static final String AUTORIDAD_CLAVE_TEMPORAL = "CLAVE_TEMPORAL";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtFiltro(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                Claims claims = jwtService.validar(cabecera.substring(PREFIJO.length()).strip());
                Integer version = claims.get("version", Integer.class);
                usuarioRepository.findByIdAndEliminadoFalse(claims.getSubject())
                        .filter(Usuario::isActivo)
                        .filter(usuario -> usuario.getVersionToken() == version)
                        .ifPresent(this::autenticar);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getNombreUsuario(),
                usuario.getRol(), usuario.isClaveTemporal());
        List<GrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority(usuario.isClaveTemporal()
                ? AUTORIDAD_CLAVE_TEMPORAL
                : "ROLE_" + usuario.getRol().name()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, autoridades));
    }
}
