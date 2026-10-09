package com.example.mascotas.cliente;

import com.example.mascotas.dto.Dto.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Cliente HTTP reutilizable. Cada llamada puede usar credenciales de un usuario distinto. */
@Component
public class TinderRestClient {
    private final RestTemplate rest;
    private final String baseUrl;
    public TinderRestClient(RestTemplate rest, @Value("${tinder.api-url:http://localhost:8080}") String baseUrl) {
        this.rest = rest; this.baseUrl = baseUrl.replaceAll("/+$", "") + "/api";
    }
    private HttpHeaders headers(String mail, String clave) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (mail != null) headers.setBasicAuth(mail, clave, StandardCharsets.UTF_8);
        return headers;
    }
    public ZonaDto crearZona(ZonaRequest request) { return rest.postForObject(baseUrl + "/zonas", request, ZonaDto.class); }
    public List<ZonaDto> zonas() {
        return rest.exchange(baseUrl + "/zonas", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ZonaDto>>() { }).getBody();
    }
    public UsuarioDto registrar(UsuarioRequest request) { return rest.postForObject(baseUrl + "/usuarios", request, UsuarioDto.class); }
    public UsuarioDto login(LoginRequest request) { return rest.postForObject(baseUrl + "/auth/login", request, UsuarioDto.class); }
    public UsuarioDto perfil(String mail, String clave) {
        return rest.exchange(baseUrl + "/usuarios/me", HttpMethod.GET, new HttpEntity<>(headers(mail, clave)), UsuarioDto.class).getBody();
    }
    public MascotaDto crearMascota(String mail, String clave, MascotaRequest request) {
        return rest.exchange(baseUrl + "/mascotas", HttpMethod.POST,
                new HttpEntity<>(request, headers(mail, clave)), MascotaDto.class).getBody();
    }
    public MascotaDto modificarMascota(String mail, String clave, String id, MascotaRequest request) {
        return rest.exchange(baseUrl + "/mascotas/{id}", HttpMethod.PUT,
                new HttpEntity<>(request, headers(mail, clave)), MascotaDto.class, id).getBody();
    }
    public void eliminarMascota(String mail, String clave, String id) {
        rest.exchange(baseUrl + "/mascotas/{id}", HttpMethod.DELETE,
                new HttpEntity<>(headers(mail, clave)), Void.class, id);
    }
    public List<MascotaDto> mascotas(String mail, String clave) {
        return rest.exchange(baseUrl + "/mascotas", HttpMethod.GET, new HttpEntity<>(headers(mail, clave)),
                new ParameterizedTypeReference<List<MascotaDto>>() { }).getBody();
    }
    public List<MascotaDto> candidatos(String mail, String clave, String id) {
        return rest.exchange(baseUrl + "/mascotas/{id}/candidatos", HttpMethod.GET, new HttpEntity<>(headers(mail, clave)),
                new ParameterizedTypeReference<List<MascotaDto>>() { }, id).getBody();
    }
    public VotoDto votar(String mail, String clave, VotoRequest request) {
        return rest.exchange(baseUrl + "/votos", HttpMethod.POST, new HttpEntity<>(request, headers(mail, clave)), VotoDto.class).getBody();
    }
    public VotoDto responder(String mail, String clave, String id) {
        return rest.exchange(baseUrl + "/votos/{id}/respuesta", HttpMethod.PUT,
                new HttpEntity<>(headers(mail, clave)), VotoDto.class, id).getBody();
    }
    public List<VotoDto> matches(String mail, String clave) {
        return rest.exchange(baseUrl + "/matches", HttpMethod.GET, new HttpEntity<>(headers(mail, clave)),
                new ParameterizedTypeReference<List<VotoDto>>() { }).getBody();
    }
}
