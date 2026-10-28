package com.example.mascotas.seguridad;

import com.example.mascotas.repositorios.UsuarioRepositorio;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Se registra exclusivamente en la cadena de Spring Security. */
public class JwtFiltro extends OncePerRequestFilter {
    private final JwtServicio jwt;
    private final UsuarioRepositorio usuarios;
    public JwtFiltro(JwtServicio jwt, UsuarioRepositorio usuarios) {
        this.jwt=jwt;
        this.usuarios=usuarios;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header!=null && header.startsWith("Bearer ")) {
            try {
                var claims = jwt.validar(header.substring(7));
                usuarios.findById(claims.getSubject()).filter(u -> u.getBaja()==null
                        && u.getTokenVersion()==claims.get("version", Integer.class)).ifPresent(u -> {
                    var auth = new UsernamePasswordAuthenticationToken(u.getId(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
