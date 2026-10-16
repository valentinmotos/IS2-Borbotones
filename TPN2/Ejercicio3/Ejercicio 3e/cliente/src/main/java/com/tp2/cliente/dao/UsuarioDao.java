package com.tp2.cliente.dao;

import com.tp2.cliente.dto.UsuarioDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class UsuarioDao {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UsuarioDao(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<UsuarioDto> listar() {
        return restTemplate.exchange(baseUrl + "/usuarios", HttpMethod.GET, null, new ParameterizedTypeReference<List<UsuarioDto>>() {}).getBody();
    }
}
