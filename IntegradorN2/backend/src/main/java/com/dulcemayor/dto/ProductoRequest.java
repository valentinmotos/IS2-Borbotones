package com.dulcemayor.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "Ingresá el código.") @Size(max = 50) String codigo,
        @NotBlank(message = "Ingresá el nombre.") @Size(max = 160) String nombre,
        @Size(max = 1000) String descripcion,
        @NotBlank(message = "Ingresá la presentación.") @Size(max = 80) String presentacion,
        @NotNull(message = "Ingresá la cantidad de unidades.") @Positive(message = "La cantidad debe ser mayor que cero.")
        Integer unidadesPorBulto,
        @NotBlank(message = "Seleccioná una categoría.") String categoriaId,
        MultipartFile imagen,
        Boolean enOferta) {
}
