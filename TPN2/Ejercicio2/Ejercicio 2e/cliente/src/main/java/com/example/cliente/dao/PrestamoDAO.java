package com.example.cliente.dao;

import com.example.cliente.dto.PrestamoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class PrestamoDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public PrestamoDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/prestamos";
    }

    public List<PrestamoDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PrestamoDTO>>() {}).getBody();
    }

    public PrestamoDTO buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, PrestamoDTO.class);
    }

    public void guardar(PrestamoDTO prestamo) {
        if (prestamo.getId() == null) {
            restTemplate.postForObject(url, prestamo, PrestamoDTO.class);
        } else {
            restTemplate.put(url + "/" + prestamo.getId(), prestamo);
        }
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
