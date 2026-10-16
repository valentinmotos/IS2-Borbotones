package com.example.servidor.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final String remitente;
    private final boolean habilitado;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:}") String remitente,
                        @Value("${app.mail.enabled:false}") boolean habilitado) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.habilitado = habilitado;
    }

    public void enviarHtml(String destinatario, String asunto, String cuerpoHtml) {
        if (!habilitado || !StringUtils.hasText(destinatario)) {
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            if (StringUtils.hasText(remitente)) {
                helper.setFrom(remitente);
            }
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            mailSender.send(mensaje);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo enviar el correo a " + destinatario, ex);
        }
    }
}
