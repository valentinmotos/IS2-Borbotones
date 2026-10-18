package com.example.cliente.dao;

import com.example.cliente.dto.PrestamoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class PrestamoDAO extends BaseDAO<PrestamoDTO> {
    public PrestamoDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl + "/prestamos", PrestamoDTO.class, PrestamoDTO[].class);
    }
}
