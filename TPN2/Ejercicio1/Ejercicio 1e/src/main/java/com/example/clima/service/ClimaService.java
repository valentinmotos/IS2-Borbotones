package com.example.clima.service;

import com.example.clima.dao.ClimaDAO;
import com.example.clima.dto.ClimaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClimaService {
    private final ClimaDAO climaDAO;

    public ClimaService(ClimaDAO climaDAO) {
        this.climaDAO = climaDAO;
    }

    public ClimaDTO buscarPorCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar una ciudad");
        }
        try {
            JsonNode json = climaDAO.buscarPorCiudad(ciudad.trim());
            ClimaDTO clima = new ClimaDTO();
            clima.setCiudad(json.path("name").asText());
            clima.setPais(json.path("sys").path("country").asText());
            clima.setTemperatura(json.path("main").path("temp").asDouble());
            clima.setSensacionTermica(json.path("main").path("feels_like").asDouble());
            clima.setHumedad(json.path("main").path("humidity").asInt());
            clima.setViento(json.path("wind").path("speed").asDouble());
            clima.setDescripcion(json.path("weather").path(0).path("description").asText());
            clima.setIcono("https://openweathermap.org/img/wn/" + json.path("weather").path(0).path("icon").asText() + "@2x.png");
            return clima;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciudad no encontrada");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "API key de OpenWeatherMap invalida o todavia no activada");
        }
    }
}
