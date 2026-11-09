package com.dulcemayor.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

/** Envío SMTP asíncrono de plantillas HTML (E1-06). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String REMITENTE_POR_DEFECTO = "no-reply@dulcemayor.com.ar";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String remitenteConfigurado;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
            @Value("${app.mail.from:}") String remitenteConfigurado) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.remitenteConfigurado = remitenteConfigurado;
    }

    /**
     * Envía una plantilla ubicada bajo {@code src/main/resources/templates}.
     * Ejemplo: {@code enviar(destino, asunto, "correo/prueba", variables)}.
     * Los errores se registran y no vuelven al flujo de negocio que solicitó el correo.
     */
    @Async
    public void enviar(String destino, String asunto, String plantilla, Map<String, Object> variables) {
        try {
            Context contexto = new Context(Locale.forLanguageTag("es-AR"));
            if (variables != null) {
                contexto.setVariables(variables);
            }
            String html = templateEngine.process(plantilla, contexto);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
            helper.setTo(destino);
            helper.setFrom(remitente());
            helper.setSubject(asunto);
            helper.setText(html, true);

            mailSender.send(mensaje);
            log.info("Se envió un correo con la plantilla '{}'.", plantilla);
        } catch (Exception error) {
            log.error("No se pudo enviar un correo con la plantilla '{}'.", plantilla, error);
        }
    }

    private String remitente() {
        return remitenteConfigurado == null || remitenteConfigurado.isBlank()
                ? REMITENTE_POR_DEFECTO
                : remitenteConfigurado.strip();
    }
}
