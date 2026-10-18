package com.example.cliente.dao;

import com.example.cliente.dto.DomicilioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class DomicilioDAO extends BaseDAO<DomicilioDTO> {
    public DomicilioDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl + "/domicilios", DomicilioDTO.class, DomicilioDTO[].class);
    }
}
