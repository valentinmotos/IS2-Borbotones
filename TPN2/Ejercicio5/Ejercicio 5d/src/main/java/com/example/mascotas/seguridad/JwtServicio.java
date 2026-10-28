package com.example.mascotas.seguridad;

import com.example.mascotas.entidades.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServicio {
    private static final String EMISOR = "tinder-mascotas";
    private final SecretKey clave;
    private final long duracion;

    public JwtServicio(@Value("${app.jwt.secret}") String secreto,
                       @Value("${app.jwt.expiration-seconds}") long duracion) {
        if (duracion <= 0) throw new IllegalArgumentException("La duracion JWT debe ser positiva");
        this.duracion = duracion;
        this.clave = secreto.isBlank() ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder().issuer(EMISOR).subject(usuario.getId())
                .id(UUID.randomUUID().toString()).issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(duracion)))
                .claim("version", usuario.getTokenVersion()).claim("tipo", "access")
                .signWith(clave, Jwts.SIG.HS256).compact();
    }

    public Claims validar(String token) {
        Claims claims = Jwts.parser().verifyWith(clave).requireIssuer(EMISOR)
                .require("tipo", "access").build().parseSignedClaims(token).getPayload();
        if (claims.getSubject()==null || claims.getExpiration()==null || claims.getIssuedAt()==null
                || claims.get("version", Integer.class)==null) {
            throw new JwtException("Token incompleto");
        }
        return claims;
    }

    public long duracion() { return duracion; }
}
