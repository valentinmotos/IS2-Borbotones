package com.example.mascotas.servicios;

import com.example.mascotas.dto.Datos.*;
import com.example.mascotas.entidades.*;
import com.example.mascotas.repositorios.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Date;

@Service
@Transactional
public class UsuarioServicio {
    private final UsuarioRepositorio usuarios;
    private final ZonaRepositorio zonas;
    public UsuarioServicio(UsuarioRepositorio usuarios, ZonaRepositorio zonas) { this.usuarios=usuarios; this.zonas=zonas; }
    /** Vinculo estable por emisor + sub, nunca por correo ni por un ID del navegador. */
    public Usuario identificar(Jwt jwt) {
        String issuer=jwt.getIssuer().toString(), subject=jwt.getSubject();
        Usuario u=usuarios.findByOauthIssuerAndOauthSubject(issuer,subject).orElseGet(() -> {
            Usuario nuevo=new Usuario(); nuevo.setOauthIssuer(issuer); nuevo.setOauthSubject(subject);
            String nombre=jwt.getClaimAsString("name");
            nuevo.setNombre(nombre==null || nombre.isBlank() ? subject : nombre);
            nuevo.setApellido(""); nuevo.setMail(jwt.getClaimAsString("email")); nuevo.setAlta(new Date());
            nuevo.setZona(zonas.findAll().stream().filter(z -> z.getNombre().equals("Centro")).findFirst()
                    .orElseThrow(() -> error(HttpStatus.SERVICE_UNAVAILABLE,"No hay zonas configuradas")));
            return usuarios.saveAndFlush(nuevo);
        });
        if (u.getBaja()!=null) throw error(HttpStatus.UNAUTHORIZED,"Usuario deshabilitado");
        return u;
    }
    public Usuario actual(String id) {
        return usuarios.findById(id).filter(u -> u.getBaja()==null)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED,"Usuario no disponible"));
    }
    public UsuarioVista perfil(String id) { return UsuarioVista.de(actual(id)); }
    public UsuarioVista actualizar(String id, Perfil p) {
        Usuario u=actual(id); u.setNombre(texto(p.nombre())); u.setApellido(texto(p.apellido()));
        u.setZona(zonas.findById(p.idZona()).orElseThrow(() -> error(HttpStatus.BAD_REQUEST,"La zona no existe")));
        return UsuarioVista.de(u);
    }
    public static String texto(String valor) {
        if (valor.isBlank()) throw error(HttpStatus.BAD_REQUEST,"El nombre no puede estar vacio");
        return valor.trim();
    }
    public static ResponseStatusException error(HttpStatus status,String mensaje) { return new ResponseStatusException(status,mensaje); }
}
