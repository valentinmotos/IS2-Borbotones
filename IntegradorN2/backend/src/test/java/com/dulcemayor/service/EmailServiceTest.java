package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;
    private MimeMessage mensaje;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine templateEngine = new SpringTemplateEngine();
        ((SpringTemplateEngine) templateEngine).setTemplateResolver(resolver);

        mensaje = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mensaje);
        emailService = new EmailService(mailSender, templateEngine, "avisos@dulcemayor.com.ar");
    }

    @Test
    void renderizaLaPlantillaDeMarcaYPreparaElCorreoHtml() throws Exception {
        emailService.enviar("manuel@example.com", "Pedido listo", "correo/prueba", Map.of(
                "nombre", "Manuel",
                "mensaje", "Tu pedido está listo.",
                "url", "https://dulcemayor.com.ar/pedidos",
                "etiquetaEnlace", "Ver pedido"));

        verify(mailSender).send(mensaje);
        assertThat(mensaje.getSubject()).isEqualTo("Pedido listo");
        assertThat(mensaje.getFrom()[0].toString()).isEqualTo("avisos@dulcemayor.com.ar");
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("manuel@example.com");
        assertThat(mensaje.getContentType()).contains("text/html");
        assertThat(mensaje.getContent().toString())
                .contains("Dulce Mayor", "Hola", "Manuel", "Tu pedido está listo.", "Ver pedido");
    }

    @Test
    void unFalloDelServidorSmtpNoSePropagaAlFlujoQuePidioElCorreo() {
        doThrow(new MailSendException("SMTP no disponible")).when(mailSender).send(mensaje);

        assertThatCode(() -> emailService.enviar("manuel@example.com", "Prueba", "correo/prueba", Map.of()))
                .doesNotThrowAnyException();
    }
}
