package com.zero.ecommerce.services;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import com.zero.ecommerce.entities.ConfiguracionCorreoEmpresa;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.utils.TextoUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía correos HTML con la cuenta SMTP de la sede central. El JavaMailSender se arma en cada
 * envío con la configuración guardada, así un cambio en la pantalla de correo aplica sin reiniciar.
 * Los templates están en templates/email/ y decoran email/base.html.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String LOGO = "static/img/logo_zero_1.png";
    private static final String TIEMPO_ESPERA_MS = "10000";

    private final ConfiguracionCorreoEmpresaService configuracionService;
    private final EmpresaService empresaService;
    private final ITemplateEngine templateEngine;

    public EmailService(ConfiguracionCorreoEmpresaService configuracionService, EmpresaService empresaService,
            ITemplateEngine templateEngine) {
        this.configuracionService = configuracionService;
        this.empresaService = empresaService;
        this.templateEngine = templateEngine;
    }

    /**
     * Envía en otro hilo: si falla, lo registra en el log y no afecta a quien lo llamó.
     * template es el nombre dentro de templates/email/, sin extensión (por ejemplo "prueba").
     */
    @Async
    public void enviar(String destinatario, String asunto, String template, Map<String, Object> variables) {
        try {
            enviarAhora(destinatario, asunto, template, variables);
            log.info("Correo '{}' enviado a {}", asunto, destinatario);
        } catch (ErrorServiceException | MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("No se pudo enviar el correo '{}' a {}: {}", asunto, destinatario, e.getMessage(), e);
        }
    }

    /**
     * Correo de prueba de la pantalla de configuración. Es sincrónico a propósito: así la pantalla
     * puede mostrar si llegó al servidor o el error que devolvió el SMTP.
     */
    public void enviarPrueba(String destinatario) throws ErrorServiceException {
        try {
            enviarAhora(destinatario, "Correo de prueba de Zero", "prueba", Map.of("destinatario", destinatario));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Falló el correo de prueba a {}: {}", destinatario, e.getMessage());
            throw new ErrorServiceException("No se pudo enviar el correo de prueba: " + causaLegible(e));
        }
    }

    private void enviarAhora(String destinatario, String asunto, String template, Map<String, Object> variables)
            throws ErrorServiceException, MessagingException, UnsupportedEncodingException {
        if (!TextoUtils.esCorreoValido(destinatario)) {
            throw new ErrorServiceException("El correo del destinatario no tiene un formato válido.");
        }
        ConfiguracionCorreoEmpresa configuracion = configuracionService.buscarConfiguracionCorreoEmpresa();
        Map<String, String> empresa = empresaService.listarDatoCorreoSedeCentral();

        Context contexto = new Context(Locale.of("es", "AR"));
        contexto.setVariables(new HashMap<>(variables == null ? Map.of() : variables));
        contexto.setVariable("empresa", empresa);
        contexto.setVariable("asunto", asunto);
        String html = templateEngine.process("email/" + template, contexto);

        JavaMailSenderImpl mailSender = crearMailSender(configuracion);
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setFrom(configuracion.getCorreo(), empresa.get("razonSocial"));
        helper.setTo(destinatario.strip());
        helper.setSubject(asunto);
        helper.setText(html, true);
        // El logo va embebido (cid:logo): los clientes de correo no pueden cargar imágenes de localhost.
        helper.addInline("logo", new ClassPathResource(LOGO), "image/png");
        mailSender.send(mensaje);
    }

    private JavaMailSenderImpl crearMailSender(ConfiguracionCorreoEmpresa configuracion) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        int puerto = Integer.parseInt(configuracion.getPuerto());
        mailSender.setHost(configuracion.getSmtp());
        mailSender.setPort(puerto);
        mailSender.setUsername(configuracion.getCorreo());
        mailSender.setPassword(configuracion.getClave());
        mailSender.setDefaultEncoding("UTF-8");
        Properties propiedades = mailSender.getJavaMailProperties();
        propiedades.put("mail.transport.protocol", "smtp");
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.starttls.enable", String.valueOf(configuracion.isTls()));
        propiedades.put("mail.smtp.starttls.required", String.valueOf(configuracion.isTls()));
        // El puerto 465 usa SSL directo en lugar de STARTTLS.
        propiedades.put("mail.smtp.ssl.enable", String.valueOf(puerto == 465));
        propiedades.put("mail.smtp.connectiontimeout", TIEMPO_ESPERA_MS);
        propiedades.put("mail.smtp.timeout", TIEMPO_ESPERA_MS);
        propiedades.put("mail.smtp.writetimeout", TIEMPO_ESPERA_MS);
        return mailSender;
    }

    // El mensaje útil suele estar en la causa más profunda ("Authentication failed", "Connection refused").
    private String causaLegible(Exception e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String mensaje = causa.getMessage();
        return mensaje == null || mensaje.isBlank() ? causa.getClass().getSimpleName() : mensaje.strip();
    }
}
