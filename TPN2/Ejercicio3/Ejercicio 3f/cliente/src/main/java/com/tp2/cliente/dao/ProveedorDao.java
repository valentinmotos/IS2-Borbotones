package com.tp2.cliente.dao;

import com.tp2.cliente.dto.ProveedorDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class ProveedorDao {
    private final RestTemplate restTemplate;
    private final String url;

    public ProveedorDao(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/proveedores";
    }

    public List<ProveedorDto> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<ProveedorDto>>() {}).getBody();
    }

    public ProveedorDto buscar(String id) {
        return restTemplate.getForObject(url + "/" + id, ProveedorDto.class);
    }

    public void guardar(ProveedorDto proveedor) {
        if (proveedor.getId() == null || proveedor.getId().isBlank()) {
            restTemplate.postForObject(url, proveedor, ProveedorDto.class);
        } else {
            restTemplate.put(url + "/" + proveedor.getId(), proveedor);
        }
    }

    public void eliminar(String id) {
        restTemplate.delete(url + "/" + id);
    }
}
