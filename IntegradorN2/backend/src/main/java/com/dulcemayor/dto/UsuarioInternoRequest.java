package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que el Jefe puede cambiar desde el ABM de personal interno. */
public record UsuarioInternoRequest(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 254, message = "El correo electrónico no puede superar los 254 caracteres.") String correo,
        @NotNull(message = "El rol es obligatorio.") RolUsuario rol,
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.") String nombre,
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres.") String apellido,
        LocalDate fechaNacimiento,
        TipoDocumento tipoDocumento,
        @Size(max = 30, message = "El documento no puede superar los 30 caracteres.") String numeroDocumento,
        @Size(max = 40, message = "El legajo no puede superar los 40 caracteres.") String legajo) {
}
