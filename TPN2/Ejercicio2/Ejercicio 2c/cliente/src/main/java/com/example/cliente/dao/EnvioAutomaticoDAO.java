package com.example.cliente.dao;

import com.example.cliente.dto.EnvioAutomaticoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class EnvioAutomaticoDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public EnvioAutomaticoDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/envios-automaticos";
    }

    public List<EnvioAutomaticoDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<EnvioAutomaticoDTO>>() {}).getBody();
    }

    public EnvioAutomaticoDTO buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, EnvioAutomaticoDTO.class);
    }

    public void guardar(EnvioAutomaticoDTO envioAutomatico) {
        if (envioAutomatico.getId() == null) {
            restTemplate.postForObject(url, envioAutomatico, EnvioAutomaticoDTO.class);
        } else {
            restTemplate.put(url + "/" + envioAutomatico.getId(), envioAutomatico);
        }
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
