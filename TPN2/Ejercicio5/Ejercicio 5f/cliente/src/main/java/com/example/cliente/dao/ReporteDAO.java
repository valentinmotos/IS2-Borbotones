package com.example.cliente.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class ReporteDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public ReporteDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/reportes";
    }

    public ResponseEntity<byte[]> personasPdf() {
        return restTemplate.getForEntity(url + "/personas-alquileres.pdf", byte[].class);
    }

    public ResponseEntity<byte[]> librosExcel() {
        return restTemplate.getForEntity(url + "/libros-disponibles.xlsx", byte[].class);
    }
}
