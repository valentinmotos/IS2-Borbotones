package com.dulcemayor.service.adapter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dulcemayor.service.CotizacionProvider;

/**
 * Patrón Adapter (E1-08) · participante: Adapter. Adapta la API HTTP de DolarAPI (Adaptee,
 * https://dolarapi.com/v1/dolares/oficial) a la interfaz CotizacionProvider (Target).
 * Ante cualquier error o demora mayor al timeout devuelve vacío y lo deja en el log.
 */
@Component
public class DolarApiAdapter implements CotizacionProvider {

    private static final Logger log = LoggerFactory.getLogger(DolarApiAdapter.class);
    private static final ZoneId MENDOZA = ZoneId.of("America/Argentina/Mendoza");

    private final RestClient restClient;

    public DolarApiAdapter(RestClient.Builder builder,
            @Value("${app.cotizacion.url:https://dolarapi.com/v1/dolares/oficial}") String apiUrl,
            @Value("${app.cotizacion.timeout-ms:3000}") long timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = builder.baseUrl(apiUrl).requestFactory(fabrica).build();
    }

    @Override
    public Optional<CotizacionExterna> obtenerVigente() {
        try {
            RespuestaDolarApi respuesta = restClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RespuestaDolarApi.class);
            if (respuesta == null || respuesta.venta() == null || respuesta.fechaActualizacion() == null) {
                log.warn("DolarAPI respondió sin venta o sin fecha de actualización.");
                return Optional.empty();
            }
            // DolarAPI informa la fecha en UTC: se pasa a la hora de Mendoza para no cambiar de día a la noche
            var dia = OffsetDateTime.parse(respuesta.fechaActualizacion())
                    .atZoneSameInstant(MENDOZA).toLocalDate();
            return Optional.of(new CotizacionExterna(dia, respuesta.venta()));
        } catch (Exception ex) {
            log.warn("No se pudo obtener la cotización de DolarAPI: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Campos que se usan del JSON de DolarAPI (el resto se ignora). */
    private record RespuestaDolarApi(Double venta, String fechaActualizacion) {
    }
}
