package com.example.cliente.dao;

import com.example.cliente.dto.LibroDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.io.IOException;

@Repository
public class LibroDAO {
    private final RestTemplate restTemplate;
    private final String url;

    public LibroDAO(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.url = baseUrl + "/libros";
    }

    public List<LibroDTO> listar() {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<LibroDTO>>() {}).getBody();
    }

    public LibroDTO buscar(Long id) {
        return restTemplate.getForObject(url + "/" + id, LibroDTO.class);
    }

    public void guardar(LibroDTO libro, MultipartFile pdf) {
        LinkedMultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        partes.add("libro", new HttpEntity<>(libro, jsonHeaders));
        if (pdf != null && !pdf.isEmpty()) {
            try {
                ByteArrayResource archivo = new ByteArrayResource(pdf.getBytes()) {
                    @Override
                    public String getFilename() {
                        return pdf.getOriginalFilename();
                    }
                };
                HttpHeaders pdfHeaders = new HttpHeaders();
                pdfHeaders.setContentType(MediaType.APPLICATION_PDF);
                partes.add("pdf", new HttpEntity<>(archivo, pdfHeaders));
            } catch (IOException ex) {
                throw new IllegalArgumentException("No se pudo leer el archivo PDF", ex);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        restTemplate.exchange(libro.getId() == null ? url : url + "/" + libro.getId(),
                libro.getId() == null ? HttpMethod.POST : HttpMethod.PUT,
                new HttpEntity<>(partes, headers), LibroDTO.class);
    }

    public ResponseEntity<byte[]> abrirPdf(Long id) {
        return restTemplate.getForEntity(url + "/" + id + "/pdf", byte[].class);
    }

    public void eliminar(Long id) {
        restTemplate.delete(url + "/" + id);
    }
}
