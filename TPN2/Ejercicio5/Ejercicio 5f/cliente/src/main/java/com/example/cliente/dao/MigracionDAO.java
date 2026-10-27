package com.example.cliente.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Repository
public class MigracionDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public MigracionDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/migracion";
    }

    public int importar(MultipartFile archivo) throws IOException {
        ByteArrayResource recurso = new ByteArrayResource(archivo.getBytes()) {
            @Override
            public String getFilename() {
                return archivo.getOriginalFilename() == null ? "migracion.txt" : archivo.getOriginalFilename();
            }
        };
        HttpHeaders cabecerasArchivo = new HttpHeaders();
        cabecerasArchivo.setContentType(MediaType.TEXT_PLAIN);
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("archivo", new HttpEntity<>(recurso, cabecerasArchivo));
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.MULTIPART_FORM_DATA);
        Integer cantidad = restTemplate.postForObject(url, new HttpEntity<>(cuerpo, cabeceras), Integer.class);
        return cantidad == null ? 0 : cantidad;
    }
}
