package com.dulcemayor.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.dulcemayor.exception.ErrorServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class GeocodificacionService {

    private static final long INTERVALO_NANOS = Duration.ofSeconds(1).toNanos();

    private final RestClient cliente;
    private final Cache<String, List<Resultado>> cache = Caffeine.newBuilder()
            .maximumSize(512)
            .expireAfterWrite(Duration.ofDays(1))
            .build();
    private long ultimoInicio = System.nanoTime() - INTERVALO_NANOS;
    private boolean buscando;

    public GeocodificacionService(RestClient.Builder builder,
            @Value("${app.geocodificacion.url:https://nominatim.openstreetmap.org}") String url) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(4));
        cliente = builder.baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "DulceMayor-IntegradorN2/1.0 (+https://github.com/valentinmotos/IS2-Borbotones)")
                .build();
    }

    public List<Resultado> buscar(String consulta) {
        String limpia = consulta.strip().replaceAll("\\s+", " ");
        if (limpia.length() < 3 || limpia.length() > 400) {
            throw new ErrorServiceException("La búsqueda debe tener entre 3 y 400 caracteres.", "q");
        }

        String clave = limpia.toLowerCase(Locale.ROOT);
        synchronized (this) {
            var guardados = cache.getIfPresent(clave);
            if (guardados != null) {
                return guardados;
            }
            long ahora = System.nanoTime();
            if (buscando || ahora - ultimoInicio < INTERVALO_NANOS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "El buscador está ocupado. Esperá un momento y volvé a intentar.");
            }
            buscando = true;
            ultimoInicio = ahora;
        }

        try {
            List<Resultado> resultados = consultar(limpia);
            cache.put(clave, resultados);
            return resultados;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos buscar la dirección. Podés reintentar o marcar el punto en el mapa.");
        } finally {
            synchronized (this) {
                buscando = false;
            }
        }
    }

    private List<Resultado> consultar(String consulta) {
        var respuesta = cliente.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("q", "{consulta}")
                        .queryParam("format", "jsonv2")
                        .queryParam("countrycodes", "ar")
                        .queryParam("accept-language", "es")
                        .queryParam("limit", 5)
                        .build(consulta))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ResultadoNominatim>>() {});
        if (respuesta == null) {
            return List.of();
        }
        return respuesta.stream()
                .filter(ResultadoNominatim::esValido)
                .map(punto -> new Resultado(punto.etiqueta(), punto.latitud(), punto.longitud()))
                .toList();
    }

    private record ResultadoNominatim(
            @JsonProperty("display_name") String etiqueta,
            @JsonProperty("lat") Double latitud,
            @JsonProperty("lon") Double longitud) {
        boolean esValido() {
            return etiqueta != null && !etiqueta.isBlank()
                    && latitud != null && Math.abs(latitud) <= 90
                    && longitud != null && Math.abs(longitud) <= 180;
        }
    }

    public record Resultado(String etiqueta, double latitud, double longitud) {}
}
