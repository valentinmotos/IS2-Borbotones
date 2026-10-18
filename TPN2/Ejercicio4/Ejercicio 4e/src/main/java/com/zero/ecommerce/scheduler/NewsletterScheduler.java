package com.zero.ecommerce.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.NewsletterService;

@Component
public class NewsletterScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsletterScheduler.class);
    private final NewsletterService newsletterService;

    public NewsletterScheduler(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    /** Se evalúa diariamente; la regla de los diez días vive en el service. */
    @Scheduled(cron = "${newsletter.cron:0 0 9 * * *}", zone = "${newsletter.zona:America/Argentina/Buenos_Aires}")
    public void enviarSiCorresponde() {
        if (!newsletterService.correspondeEnviarHoy()) {
            return;
        }
        try {
            newsletterService.enviar();
        } catch (ErrorServiceException e) {
            log.warn("No se envió el newsletter programado: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado al enviar el newsletter programado.", e);
        }
    }
}
