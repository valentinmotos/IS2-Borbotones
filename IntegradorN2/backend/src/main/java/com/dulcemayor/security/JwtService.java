package com.dulcemayor.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dulcemayor.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida los JWT de la API (E1-01), adaptado de TPN2 Ej. 5d (JwtServicio).
 * Token firmado con HS256 y con vencimiento (control de sesiones). Claims:
 * sub = id del usuario, rol, version (versionToken del usuario, para cerrar sesión) y tipo "access".
 *
 * El secreto sale de JWT_SECRET (al menos 32 caracteres). Si está vacío se genera una clave al azar
 * en cada arranque: sirve para desarrollo, pero reiniciar el servidor invalida todas las sesiones.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String EMISOR = "dulce-mayor";
    private static final long DURACION_POR_DEFECTO_SEGUNDOS = 3600;
    private static final int LARGO_MINIMO_SECRETO = 32;

    private final SecretKey clave;
    private final long duracionSegundos;

    public JwtService(@Value("${app.jwt.secret:}") String secreto,
            @Value("${app.jwt.expiracion-segundos:}") String expiracion) {
        this.duracionSegundos = leerDuracion(expiracion);
        this.clave = crearClave(secreto);
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(usuario.getId())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(duracionSegundos)))
                .claim("rol", usuario.getRol().name())
                .claim("version", usuario.getVersionToken())
                .claim("tipo", "access")
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica firma, emisor y vencimiento. Lanza JwtException si el token no sirve
     * (el filtro la atrapa y la petición sigue sin autenticar: 401).
     */
    public Claims validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .require("tipo", "access")
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getSubject() == null || claims.getExpiration() == null
                || claims.get("version", Integer.class) == null) {
            throw new JwtException("Token incompleto");
        }
        return claims;
    }

    public long getDuracionSegundos() {
        return duracionSegundos;
    }

    private static long leerDuracion(String expiracion) {
        if (expiracion == null || expiracion.isBlank()) {
            return DURACION_POR_DEFECTO_SEGUNDOS;
        }
        long segundos;
        try {
            segundos = Long.parseLong(expiracion.strip());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("JWT_EXPIRATION tiene que ser una cantidad de segundos.");
        }
        if (segundos <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION tiene que ser mayor que cero.");
        }
        return segundos;
    }

    private static SecretKey crearClave(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            log.warn("JWT_SECRET no está configurado: se usa una clave al azar. "
                    + "Las sesiones se pierden al reiniciar el servidor. Configurarlo en producción.");
            return Jwts.SIG.HS256.key().build();
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException("JWT_SECRET tiene que tener al menos " + LARGO_MINIMO_SECRETO
                    + " caracteres.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
