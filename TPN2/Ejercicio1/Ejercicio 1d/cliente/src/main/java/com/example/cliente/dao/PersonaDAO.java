package com.example.cliente.dao;

import com.example.cliente.dto.PersonaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class PersonaDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public PersonaDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/personas";
    }

    public List<PersonaDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<PersonaDTO>>() {}).getBody();
    }

    public PersonaDTO buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, PersonaDTO.class);
    }

    public void guardar(PersonaDTO persona) {
        if (persona.getId() == null) {
            restTemplate.postForObject(url, persona, PersonaDTO.class);
        } else {
            restTemplate.put(url + "/" + persona.getId(), persona);
        }
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
