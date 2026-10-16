package com.example.cliente.dao;

import com.example.cliente.dto.LocalidadDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class LocalidadDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public LocalidadDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/localidades";
    }

    public List<LocalidadDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<LocalidadDTO>>() {}).getBody();
    }
}
