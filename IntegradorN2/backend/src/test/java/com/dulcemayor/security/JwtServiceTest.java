package com.dulcemayor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** E1-01: emisión y validación del JWT. */
class JwtServiceTest {

    private static final String SECRETO = "un-secreto-de-prueba-con-mas-de-32-caracteres";

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId("id-1");
        usuario.setRol(RolUsuario.PREVENTOR);
        usuario.setVersionToken(3);
        return usuario;
    }

    @Test
    void elTokenEmitidoSeValidaConSusClaims() {
        JwtService jwt = new JwtService(SECRETO, "600");

        Claims claims = jwt.validar(jwt.emitir(usuario()));

        assertThat(claims.getSubject()).isEqualTo("id-1");
        assertThat(claims.get("rol", String.class)).isEqualTo("PREVENTOR");
        assertThat(claims.get("version", Integer.class)).isEqualTo(3);
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(600_000);
    }

    @Test
    void unTokenFirmadoConOtraClaveOAlteradoSeRechaza() {
        String token = new JwtService("otro-secreto-distinto-tambien-de-32-caracteres", "600").emitir(usuario());
        JwtService jwt = new JwtService(SECRETO, "600");

        assertThatThrownBy(() -> jwt.validar(token)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.validar(jwt.emitir(usuario()) + "x")).isInstanceOf(JwtException.class);
    }

    /** E6-02: sesión vencida. Un token firmado con la clave correcta pero ya expirado se rechaza. */
    @Test
    void unTokenVencidoSeRechaza() {
        JwtService jwt = new JwtService(SECRETO, "600");
        long ahora = System.currentTimeMillis();
        String vencido = Jwts.builder().subject("id-1").claim("rol", "PREVENTOR").claim("version", 3)
                .issuedAt(new java.util.Date(ahora - 7_200_000)).expiration(new java.util.Date(ahora - 3_600_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwt.validar(vencido)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void sinExpiracionConfiguradaDuraUnaHora() {
        assertThat(new JwtService("", "").getDuracionSegundos()).isEqualTo(3600);
    }

    @Test
    void unSecretoCortoOUnaExpiracionInvalidaNoArrancan() {
        assertThatThrownBy(() -> new JwtService("corto", "600"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 caracteres");
        assertThatThrownBy(() -> new JwtService(SECRETO, "-5"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(SECRETO, "una hora"))
                .isInstanceOf(IllegalStateException.class);
    }
}
