package com.zero.ecommerce.dto;

import java.time.LocalDate;

public record NewsletterEnvioResultadoDTO(int cantidadEnviada, LocalDate fechaEnvio, LocalDate proximoEnvio) {
}
