package com.zero.ecommerce.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zero.ecommerce.services.VigenciaPrecioService;

@Component
public class PrecioScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrecioScheduler.class);
    private final VigenciaPrecioService vigenciaPrecioService;

    public PrecioScheduler(VigenciaPrecioService vigenciaPrecioService) {
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    /** Ejecuta diariamente la deteccion requerida por RF12. */
    @Scheduled(cron = "${precios.alertas.cron:0 15 9 * * *}",
            zone = "${precios.alertas.zona:America/Argentina/Buenos_Aires}")
    public void detectarPreciosVencidos() {
        try {
            int cantidad = vigenciaPrecioService.listarProductosConPrecioVencido().size();
            log.info("Deteccion diaria de precios finalizada: {} producto(s) requieren actualizacion.", cantidad);
        } catch (RuntimeException e) {
            log.error("No se pudo completar la deteccion diaria de precios vencidos.", e);
        }
    }
}
