package com.example.cliente.dao;

import com.example.cliente.dto.LibroDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class LibroDAO extends BaseDAO<LibroDTO> {
    public LibroDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        super(restTemplate, baseUrl + "/libros", LibroDTO.class, LibroDTO[].class);
    }
}
