package com.example.mascotas.dto;

import com.example.mascotas.enumeracion.Sexo;
import com.example.mascotas.enumeracion.Tipo;
import jakarta.validation.constraints.*;
import java.util.Date;

/** Contratos JSON: las respuestas no exponen claves, entidades ni bytes de fotos. */
public final class Dto {
    private Dto() { }

    public record ZonaRequest(@NotBlank @Size(max = 100) String nombre,
                              @Size(max = 500) String descripcion) { }
    public record ZonaDto(String id, String nombre, String descripcion) { }
    public record UsuarioRequest(@NotBlank @Size(max = 100) String nombre,
                                 @NotBlank @Size(max = 100) String apellido,
                                 @NotBlank @Email @Size(max = 200) String mail,
                                 @NotBlank @Size(min = 7, max = 72) String clave,
                                 @NotBlank String clave2, @NotBlank String zonaId) {
        @Override public String toString() { return "UsuarioRequest[mail=" + mail + ", clave=***, clave2=***]"; }
    }
    public record UsuarioDto(String id, String nombre, String apellido, String mail,
                             String zonaId, String fotoId, Date alta, Date baja) { }
    public record LoginRequest(@NotBlank @Email String mail, @NotBlank String clave) {
        @Override public String toString() { return "LoginRequest[mail=" + mail + ", clave=***]"; }
    }
    public record MascotaRequest(@NotBlank @Size(max = 100) String nombre,
                                 @NotNull Sexo sexo, @NotNull Tipo tipo) { }
    public record MascotaDto(String id, String nombre, Sexo sexo, Tipo tipo,
                             String usuarioId, String usuarioNombre, String usuarioApellido,
                             String zonaId, String fotoId, Date alta, Date baja) { }
    public record VotoRequest(@NotBlank String mascota1Id, @NotBlank String mascota2Id) { }
    public record VotoDto(String id, String mascota1Id, String mascota2Id,
                          Date fecha, Date respuesta, boolean match) { }
    public record FotoDto(String id, String nombre, String mime) { }
}
