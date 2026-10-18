package com.example.cliente.dao;

import com.example.cliente.dto.BaseDTO;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public abstract class BaseDAO<D extends BaseDTO> {
    private final RestTemplate restTemplate;
    private final String url;
    private final Class<D> tipo;
    private final Class<D[]> tipoLista;

    protected BaseDAO(RestTemplate restTemplate, String url, Class<D> tipo, Class<D[]> tipoLista) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.tipo = tipo;
        this.tipoLista = tipoLista;
    }

    public List<D> listar() {
        return Arrays.asList(restTemplate.getForObject(url, tipoLista));
    }

    public D buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, tipo);
    }

    public void guardar(D dto) {
        if (dto.getId() == null) {
            restTemplate.postForObject(url, dto, tipo);
        } else {
            restTemplate.put(url + "/" + dto.getId(), dto);
        }
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
