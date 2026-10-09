package com.example.clima.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Repository
public class ClimaDAO {
    private final RestTemplate restTemplate;
    private final String url;
    private final String apiKey;

    public ClimaDAO(RestTemplate restTemplate,
                    @Value("${openweather.api.url}") String url,
                    @Value("${openweather.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.apiKey = apiKey;
    }

    public JsonNode buscarPorCiudad(String ciudad) {
        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("q", ciudad)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "es")
                .toUriString();
        return restTemplate.getForObject(uri, JsonNode.class);
    }
}
