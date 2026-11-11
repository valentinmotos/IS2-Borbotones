package com.dulcemayor.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * E2-07: datos que el minorista edita en su perfil. El documento y el correo no se modifican acá.
 * La dirección de entrega llega con los campos del componente de E1-04.
 */
public record PerfilMinoristaRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.") String nombre,
        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres.") String apellido,
        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        @Past(message = "Ingresá una fecha de nacimiento válida.") LocalDate fechaNacimiento,
        @NotBlank(message = "Seleccioná la localidad de entrega.") String localidadId,
        @NotBlank(message = "Ingresá la calle.")
        @Size(max = 160, message = "La calle admite hasta 160 caracteres.") String calle,
        @NotBlank(message = "Ingresá la numeración.")
        @Size(max = 30, message = "La numeración admite hasta 30 caracteres.") String numeracion,
        @Size(max = 120, message = "El barrio admite hasta 120 caracteres.") String barrio,
        @Size(max = 80, message = "Manzana o piso admite hasta 80 caracteres.") String manzanaPiso,
        @Size(max = 80, message = "Casa o departamento admite hasta 80 caracteres.") String casaDepartamento,
        @Size(max = 500, message = "La referencia admite hasta 500 caracteres.") String referencia,
        @NotNull(message = "Marcá la ubicación de entrega en el mapa.")
        @DecimalMin(value = "-90.0", message = "La latitud no es válida.")
        @DecimalMax(value = "90.0", message = "La latitud no es válida.") Double latitud,
        @NotNull(message = "Marcá la ubicación de entrega en el mapa.")
        @DecimalMin(value = "-180.0", message = "La longitud no es válida.")
        @DecimalMax(value = "180.0", message = "La longitud no es válida.") Double longitud) {
}
