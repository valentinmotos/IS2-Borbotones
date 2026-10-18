package com.example.cliente.dao;

import com.example.cliente.dto.PersonaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class PersonaDAO extends BaseDAO<PersonaDTO> {
    public PersonaDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl + "/personas", PersonaDTO.class, PersonaDTO[].class);
    }
}
