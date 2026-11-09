package com.dulcemayor.scheduler;

import com.dulcemayor.service.CotizacionDolarService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CotizacionDolarScheduler {

    private final CotizacionDolarService cotizacionDolarService;

    public CotizacionDolarScheduler(CotizacionDolarService cotizacionDolarService) {
        this.cotizacionDolarService = cotizacionDolarService;
    }

    @Scheduled(fixedDelayString = "${cotizacion.dolar.intervalo-ms:3600000}", initialDelay = 0)
    public void actualizarCotizacionCadaHora() {
        try {
            cotizacionDolarService.actualizarCotizacion();
        } catch (RuntimeException ex) {
            // Si no hay proveedor ni cotización guardada, la próxima ejecución volverá a intentarlo.
        }
    }
}
