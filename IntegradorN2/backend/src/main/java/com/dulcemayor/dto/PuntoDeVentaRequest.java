package com.dulcemayor.dto;

import jakarta.validation.constraints.*;

public record PuntoDeVentaRequest(
        @NotBlank @Size(max = 160) String nombreComercio,
        @NotBlank String minoristaId,
        @NotBlank String localidadId,
        @NotBlank @Size(max = 160) String calle,
        @NotBlank @Size(max = 30) String numeracion,
        @Size(max = 120) String barrio,
        @Size(max = 80) String manzanaPiso,
        @Size(max = 80) String casaDepartamento,
        @Size(max = 500) String referencia,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitud,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitud) {}
