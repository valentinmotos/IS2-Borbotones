package com.example.cliente.dao;

import com.example.cliente.dto.LocalidadDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class LocalidadDAO extends BaseDAO<LocalidadDTO> {
    public LocalidadDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl + "/localidades", LocalidadDTO.class, LocalidadDTO[].class);
    }
}
