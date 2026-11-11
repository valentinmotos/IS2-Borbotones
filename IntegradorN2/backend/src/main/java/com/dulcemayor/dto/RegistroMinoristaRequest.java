package com.dulcemayor.dto;

import java.time.LocalDate;
import com.dulcemayor.entity.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegistroMinoristaRequest(
        @NotBlank(message = "El nombre es obligatorio.") @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.") String nombre,
        @NotBlank(message = "El apellido es obligatorio.") @Size(max = 100, message = "El apellido no puede superar los 100 caracteres.") String apellido,
        @NotNull(message = "La fecha de nacimiento es obligatoria.") @Past(message = "Ingresá una fecha de nacimiento válida.") LocalDate fechaNacimiento,
        @NotNull(message = "El tipo de documento es obligatorio.") TipoDocumento tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio.") @Size(max = 30, message = "El documento no puede superar los 30 caracteres.") String numeroDocumento,
        @NotBlank(message = "El correo electrónico es obligatorio.") @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 254, message = "El correo electrónico no puede superar los 254 caracteres.") String correo,
        @NotBlank(message = "La clave es obligatoria.") @Size(min = 8, message = "La clave tiene que tener al menos 8 caracteres.") String clave) {
}
