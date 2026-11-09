package com.dulcemayor.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dulcemayor.service.CotizacionDolarService;

/**
 * Actualiza la cotización del dólar cada hora (E1-08). No corre en el perfil "test", así los
 * tests no salen a internet. Arranca un minuto después de iniciar para no competir con el DataSeeder
 * (que solo carga datos si la base está vacía).
 */
@Component
@Profile("!test")
public class CotizacionDolarScheduler {

    private static final Logger log = LoggerFactory.getLogger(CotizacionDolarScheduler.class);

    private final CotizacionDolarService cotizacionDolarService;

    public CotizacionDolarScheduler(CotizacionDolarService cotizacionDolarService) {
        this.cotizacionDolarService = cotizacionDolarService;
    }

    @Scheduled(fixedDelayString = "${app.cotizacion.intervalo-ms:3600000}", initialDelay = 60_000)
    public void actualizarCotizacionCadaHora() {
        try {
            cotizacionDolarService.actualizarCotizacion();
        } catch (RuntimeException ex) {
            log.warn("No se pudo actualizar la cotización del dólar: {}", ex.getMessage());
        }
    }
}
