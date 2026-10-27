package com.example.cliente.dao;

import com.example.cliente.dto.LoginDTO;
import com.example.cliente.dto.TokenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class AuthDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public AuthDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/auth/login";
    }

    public TokenDTO login(LoginDTO login) {
        return restTemplate.postForObject(url, login, TokenDTO.class);
    }
}
