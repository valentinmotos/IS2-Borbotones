package com.example.cliente.dao;

import com.example.cliente.dto.EnvioAutomaticoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class EnvioAutomaticoDAO extends BaseDAO<EnvioAutomaticoDTO> {
    public EnvioAutomaticoDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl + "/envios-automaticos", EnvioAutomaticoDTO.class, EnvioAutomaticoDTO[].class);
    }
}
