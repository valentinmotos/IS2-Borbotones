package com.zero.ecommerce.dto;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteDTO {

    private String id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidadId;

    // Contacto telefónico (Celular)
    @NotBlank(message = "El número de teléfono es obligatorio")
    private String telefonoCelular;

    // Foto de perfil opcional
    private MultipartFile fotoPerfil;
    private String imagenUrl;

    // Fragment Dirección (E1-04)
    private String calle;
    private String numeracion;
    private String barrio;
    private String manzanaPiso;
    private String casaDepartamento;
    private String referencia;

    @NotBlank(message = "La localidad es obligatoria")
    private String localidadId;
}