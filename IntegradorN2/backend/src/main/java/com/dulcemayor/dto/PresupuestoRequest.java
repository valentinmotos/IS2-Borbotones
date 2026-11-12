package com.dulcemayor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** E3-04: mensaje que el visitante escribe en el chat, por ejemplo «quiero 10 alfajores y 5 chupetines». */
public record PresupuestoRequest(
        @NotBlank(message = "Escribí qué productos querés presupuestar.")
        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres.")
        String mensaje) {
}
