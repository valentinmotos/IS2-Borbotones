package com.example.mascotas.servicios;

import com.example.mascotas.dto.Datos.*;
import com.example.mascotas.entidades.*;
import com.example.mascotas.repositorios.*;
import com.example.mascotas.seguridad.JwtServicio;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Transactional
public class UsuarioServicio {
    private final UsuarioRepositorio usuarios;
    private final ZonaRepositorio zonas;
    private final PasswordEncoder encoder;
    private final JwtServicio jwt;
    private final String hashFicticio;

    public UsuarioServicio(UsuarioRepositorio usuarios, ZonaRepositorio zonas, PasswordEncoder encoder, JwtServicio jwt) {
        this.usuarios=usuarios; this.zonas=zonas; this.encoder=encoder; this.jwt=jwt;
        this.hashFicticio=encoder.encode(UUID.randomUUID().toString());
    }
    public AuthRespuesta registrar(Registro r) {
        if (!r.clave().equals(r.clave2())) throw error(HttpStatus.BAD_REQUEST, "Las claves deben coincidir");
        // BCrypt tiene un limite de 72 bytes, que no equivale a 72 caracteres Unicode.
        if (r.clave().getBytes(StandardCharsets.UTF_8).length>72) throw error(HttpStatus.BAD_REQUEST, "La clave supera los 72 bytes");
        String mail = r.mail().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByMail(mail)) throw error(HttpStatus.CONFLICT, "El mail ya esta registrado");
        Usuario u = new Usuario();
        u.setNombre(texto(r.nombre())); u.setApellido(texto(r.apellido())); u.setMail(mail);
        u.setZona(zona(r.idZona())); u.setClave(encoder.encode(r.clave())); u.setAlta(new Date());
        usuarios.saveAndFlush(u);
        return respuesta(u);
    }
    @Transactional(readOnly=true)
    public AuthRespuesta login(Login r) {
        if (r.clave().getBytes(StandardCharsets.UTF_8).length>72)
            throw error(HttpStatus.UNAUTHORIZED, "Mail o clave incorrectos");
        Usuario u = usuarios.findByMail(r.mail().trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean coincide = encoder.matches(r.clave(), u==null ? hashFicticio : u.getClave());
        if (!coincide || u==null || u.getBaja()!=null) throw error(HttpStatus.UNAUTHORIZED, "Mail o clave incorrectos");
        return respuesta(u);
    }
    public void logout(String id) {
        Usuario u = actual(id);
        u.setTokenVersion(u.getTokenVersion()+1);
    }
    public Usuario actual(String id) {
        return usuarios.findById(id).filter(u -> u.getBaja()==null)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Usuario no disponible"));
    }
    public UsuarioVista perfil(String id) { return UsuarioVista.de(actual(id)); }
    public UsuarioVista actualizar(String id, Perfil p) {
        Usuario u=actual(id);
        u.setNombre(texto(p.nombre())); u.setApellido(texto(p.apellido())); u.setZona(zona(p.idZona()));
        return UsuarioVista.de(u);
    }
    private AuthRespuesta respuesta(Usuario u) {
        return new AuthRespuesta(jwt.emitir(u), "Bearer", jwt.duracion(), UsuarioVista.de(u));
    }
    private Zona zona(String id) {
        return zonas.findById(id).orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "La zona no existe"));
    }
    public static String texto(String valor) {
        if (valor.isBlank()) throw error(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacio");
        return valor.trim();
    }
    public static ResponseStatusException error(HttpStatus estado, String mensaje) { return new ResponseStatusException(estado, mensaje); }
}
