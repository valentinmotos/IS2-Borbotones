package com.dulcemayor.dto;

import jakarta.validation.constraints.Size;

/**
 * E5-02: datos opcionales de la entrega. La posición (GPS del celular) se usa para proponer el siguiente
 * punto; sin ella se parte del comercio recién entregado.
 */
public record EntregaRequest(Double latitud, Double longitud,
        @Size(max = 500, message = "La observación admite hasta 500 caracteres.") String observacion) {
}
