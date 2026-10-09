package com.example.cliente.dao;

import com.example.cliente.dto.LibroDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class LibroDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public LibroDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/libros";
    }

    public List<LibroDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<LibroDTO>>() {}).getBody();
    }

    public LibroDTO buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, LibroDTO.class);
    }

    public void guardar(LibroDTO libro) {
        if (libro.getId() == null) {
            restTemplate.postForObject(url, libro, LibroDTO.class);
        } else {
            restTemplate.put(url + "/" + libro.getId(), libro);
        }
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
