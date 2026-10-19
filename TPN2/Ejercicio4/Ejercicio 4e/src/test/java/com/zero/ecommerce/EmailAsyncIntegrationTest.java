package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zero.ecommerce.repositories.ConfiguracionCorreoEmpresaRepository;
import com.zero.ecommerce.services.ConfiguracionCorreoEmpresaService;
import com.zero.ecommerce.services.EmailService;
import com.zero.ecommerce.services.EmpresaService;

import jakarta.mail.internet.MimeMessage;

/**
 * EmailService.enviar corre en otro hilo (@Async). Este test no es @Transactional a propósito:
 * SQLite usa una sola conexión y el hilo del envío tiene que leer la configuración ya guardada.
 * Por eso borra lo que crea al terminar.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
class EmailAsyncIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser(CorreoIntegrationTest.CUENTA, CorreoIntegrationTest.CLAVE));

    private final EmailService emailService;
    private final ConfiguracionCorreoEmpresaService configuracionService;
    private final ConfiguracionCorreoEmpresaRepository configuracionRepository;
    private final EmpresaService empresaService;

    EmailAsyncIntegrationTest(@Autowired EmailService emailService,
            @Autowired ConfiguracionCorreoEmpresaService configuracionService,
            @Autowired ConfiguracionCorreoEmpresaRepository configuracionRepository,
            @Autowired EmpresaService empresaService) {
        this.emailService = emailService;
        this.configuracionService = configuracionService;
        this.configuracionRepository = configuracionRepository;
        this.empresaService = empresaService;
    }

    @AfterEach
    void borrarConfiguracion() {
        configuracionRepository.deleteAll();
    }

    @Test
    void enviarEsAsincronicoYElCorreoLlega() throws Exception {
        configuracionService.crearConfiguracionCorreoEmpresa(CorreoIntegrationTest.CUENTA, CorreoIntegrationTest.CLAVE,
                String.valueOf(ServerSetupTest.SMTP.getPort()), "127.0.0.1", false,
                empresaService.buscarSedeCentral().getId());

        emailService.enviar("cliente@test.com", "Correo de prueba de Zero", "prueba",
                Map.of("destinatario", "cliente@test.com"));

        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage recibido = GREEN_MAIL.getReceivedMessages()[0];
        assertThat(recibido.getSubject()).isEqualTo("Correo de prueba de Zero");
        assertThat(recibido.getAllRecipients()[0].toString()).isEqualTo("cliente@test.com");
    }

    @Test
    void siFallaNoRompeAQuienLoLlama() {
        // Sin configuración de correo: el error queda en el log y la llamada termina normalmente.
        assertThatCode(() -> emailService.enviar("cliente@test.com", "Asunto", "prueba", Map.of()))
                .doesNotThrowAnyException();
        assertThat(GREEN_MAIL.waitForIncomingEmail(1_000, 1)).isFalse();
    }
}
