package com.dulcemayor.scheduler;

import com.dulcemayor.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsletterScheduler {
    private static final Logger log = LoggerFactory.getLogger(NewsletterScheduler.class);

    private final NewsletterService newsletter;

    @Scheduled(cron = "${app.newsletter.cron:0 0 9 5 * *}", zone = "America/Argentina/Mendoza")
    public void enviarOfertasDelMes() {
        var resultado = newsletter.enviarOfertas();
        log.info("Newsletter mensual: {} ofertas, {} destinatarios programados.",
                resultado.ofertas(), resultado.destinatariosProgramados());
    }
}
