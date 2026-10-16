package com.tp2.cliente.dao;

import com.tp2.cliente.dto.ArticuloDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class ArticuloDao {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ArticuloDao(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<ArticuloDto> listar() {
        return restTemplate.exchange(baseUrl + "/articulos", HttpMethod.GET, null, new ParameterizedTypeReference<List<ArticuloDto>>() {}).getBody();
    }
}
