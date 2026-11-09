package com.dulcemayor.service.adapter;

import com.dulcemayor.service.CotizacionProvider;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Adapta el contrato HTTP de DolarAPI al puerto CotizacionProvider de la aplicación. */
@Component
public class DolarApiAdapter implements CotizacionProvider {

    private final RestClient restClient;

    public DolarApiAdapter(RestClient.Builder builder,
            @Value("${COTIZACION_DOLAR_API:https://dolarapi.com/v1/dolares/oficial}") String apiUrl) {
        this.restClient = builder.baseUrl(apiUrl).build();
    }

    @Override
    public Optional<CotizacionExterna> obtenerCotizacionOficial() {
        try {
            RespuestaDolarApi respuesta = restClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RespuestaDolarApi.class);
            if (respuesta == null || respuesta.venta() == null || respuesta.fechaActualizacion() == null) {
                return Optional.empty();
            }
            OffsetDateTime fechaActualizacion = OffsetDateTime.parse(respuesta.fechaActualizacion());
            return Optional.of(new CotizacionExterna(fechaActualizacion.toLocalDateTime(), respuesta.venta()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private record RespuestaDolarApi(Double venta, String fechaActualizacion) {
    }
}
