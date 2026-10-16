package com.example.servidor.dto;

public record LibroDTO(Long id, String titulo, Integer fecha, String genero, Integer paginas,
                       String autor, String tipo, String formato, String urlDescarga) {
}
