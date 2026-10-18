package com.zero.ecommerce.dto;

import java.time.LocalDate;
import java.util.List;

public record NewsletterEstadoDTO(
        List<ProductoCatalogoDTO> ofertas,
        int cantidadDestinatarios,
        LocalDate ultimoEnvio,
        LocalDate proximoEnvio) {
}
