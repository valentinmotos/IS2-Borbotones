package com.example.cliente.dao;

import com.example.cliente.dto.DomicilioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class DomicilioDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public DomicilioDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/domicilios";
    }

    public List<DomicilioDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<DomicilioDTO>>() {}).getBody();
    }

    public DomicilioDTO buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, DomicilioDTO.class);
    }

    public void guardar(DomicilioDTO domicilio) {
        restTemplate.put(url + "/" + domicilio.getId(), domicilio);
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
