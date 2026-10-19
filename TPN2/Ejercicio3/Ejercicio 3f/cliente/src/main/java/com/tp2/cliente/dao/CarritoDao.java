package com.tp2.cliente.dao;

import com.tp2.cliente.dto.AgregarArticuloRequest;
import com.tp2.cliente.dto.CarritoDto;
import com.tp2.cliente.dto.CrearCarritoRequest;
import com.tp2.cliente.dto.EditarDetalleRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Repository
public class CarritoDao {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CarritoDao(RestTemplate restTemplate, @Value("${servidor.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<CarritoDto> listar() {
        return restTemplate.exchange(baseUrl + "/carritos", HttpMethod.GET, null, new ParameterizedTypeReference<List<CarritoDto>>() {}).getBody();
    }

    public CarritoDto crear(String usuarioId) {
        return restTemplate.postForObject(baseUrl + "/carritos", new CrearCarritoRequest(usuarioId), CarritoDto.class);
    }

    public CarritoDto agregarArticulo(String carritoId, String articuloId, int cantidad) {
        return restTemplate.postForObject(baseUrl + "/carritos/" + carritoId + "/detalles", new AgregarArticuloRequest(articuloId, cantidad), CarritoDto.class);
    }

    public void editarDetalle(String carritoId, String detalleId, int cantidad) {
        restTemplate.put(baseUrl + "/carritos/" + carritoId + "/detalles/" + detalleId, new EditarDetalleRequest(cantidad));
    }

    public void eliminar(String carritoId) {
        restTemplate.delete(baseUrl + "/carritos/" + carritoId);
    }

    public CarritoDto finalizar(String carritoId) {
        return restTemplate.postForObject(baseUrl + "/carritos/" + carritoId + "/finalizar", null, CarritoDto.class);
    }
}
