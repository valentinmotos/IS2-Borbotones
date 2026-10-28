package com.example.mascotas.dto;

import com.example.mascotas.entidades.*;
import com.example.mascotas.enumeracion.*;
import jakarta.validation.constraints.*;
import java.util.Date;

/** Contratos de la API: nunca se serializan entidades ni hashes de contrasenas. */
public final class Datos {
    private Datos() {}
    public record Registro(@NotBlank @Size(max=80) String nombre,
            @NotBlank @Size(max=80) String apellido,
            @NotBlank @Email @Size(max=254) String mail,
            @NotBlank @Size(min=8, max=64) String clave,
            @NotBlank String clave2, @NotBlank String idZona) {
        @Override public String toString() { return "Registro[credenciales protegidas]"; }
    }
    public record Login(@NotBlank @Email String mail, @NotBlank @Size(max=64) String clave) {
        @Override public String toString() { return "Login[credenciales protegidas]"; }
    }
    public record Perfil(@NotBlank @Size(max=80) String nombre,
            @NotBlank @Size(max=80) String apellido, @NotBlank String idZona) {}
    public record MascotaEntrada(@NotBlank @Size(max=80) String nombre,
            @NotNull Sexo sexo, @NotNull Tipo tipo) {}
    public record VotoEntrada(@NotBlank String idMascotaPropia, @NotBlank String idMascotaVotada) {}
    public record ZonaVista(String id, String nombre) {
        public static ZonaVista de(Zona z) { return new ZonaVista(z.getId(), z.getNombre()); }
    }
    public record UsuarioVista(String id, String nombre, String apellido, String mail, ZonaVista zona, boolean tieneFoto) {
        public static UsuarioVista de(Usuario u) {
            return new UsuarioVista(u.getId(), u.getNombre(), u.getApellido(), u.getMail(), ZonaVista.de(u.getZona()), u.getFoto()!=null);
        }
    }
    public record AuthRespuesta(String accessToken, String tokenType, long expiresIn, UsuarioVista usuario) {
        @Override public String toString() { return "AuthRespuesta[token protegido]"; }
    }
    public record MascotaVista(String id, String nombre, Sexo sexo, Tipo tipo, String dueno, ZonaVista zona, boolean tieneFoto) {
        public static MascotaVista de(Mascota m) {
            return new MascotaVista(m.getId(), m.getNombre(), m.getSexo(), m.getTipo(),
                    m.getUsuario().getNombre(), ZonaVista.de(m.getUsuario().getZona()), m.getFoto()!=null);
        }
    }
    public record VotoVista(String id, MascotaVista origen, MascotaVista destino, Date fecha, boolean match, boolean recibido) {
        public static VotoVista de(Voto v, String usuario) {
            return new VotoVista(v.getId(), MascotaVista.de(v.getMascota1()), MascotaVista.de(v.getMascota2()),
                    v.getFecha(), v.getRespuesta()!=null, v.getMascota2().getUsuario().getId().equals(usuario));
        }
    }
}
