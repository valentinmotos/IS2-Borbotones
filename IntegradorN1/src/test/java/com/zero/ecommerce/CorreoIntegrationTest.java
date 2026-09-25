package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zero.ecommerce.entities.ConfiguracionCorreoEmpresa;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ConfiguracionCorreoEmpresaService;
import com.zero.ecommerce.services.EmailService;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * Pantalla de correo y envío real contra GreenMail, un servidor SMTP en memoria (puerto 3025):
 * no sale ningún correo de la máquina.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
@Transactional
class CorreoIntegrationTest {

    static final String CUENTA = "zero@test.com";
    static final String CLAVE = "clave-secreta";

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser(CUENTA, CLAVE));

    private static final String BASE = "/admin/configuracion/correo";
    private final MockMvc mvc;
    private final ConfiguracionCorreoEmpresaService configuracionService;
    private final EmailService emailService;
    private MockHttpSession sesionJefe;

    CorreoIntegrationTest(@Autowired MockMvc mvc, @Autowired ConfiguracionCorreoEmpresaService configuracionService,
            @Autowired EmailService emailService) {
        this.mvc = mvc;
        this.configuracionService = configuracionService;
        this.emailService = emailService;
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        sesionJefe = login("jefe@zero.com.ar", "Jefe123!");
    }

    @Test
    void sinConfiguracionNoSePuedeEnviarLaPrueba() throws Exception {
        mvc.perform(get(BASE).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Guardá la cuenta de envío")))
                .andExpect(content().string(containsString("value=\"smtp.gmail.com\"")))
                .andExpect(content().string(not(containsString("Enviar correo de prueba</button>"))));
        assertThatThrownBy(() -> emailService.enviarPrueba("destino@test.com"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("Todavía no se configuró el correo de la empresa.");
    }

    @Test
    void guardaLaConfiguracionYElCorreoDePruebaLlegaConElDisenoDeLaMarca() throws Exception {
        guardarConfiguracion(ServerSetupTest.SMTP.getPort());

        ConfiguracionCorreoEmpresa guardada = configuracionService.buscarConfiguracionCorreoEmpresa();
        assertThat(guardada.getClave()).isEqualTo(CLAVE);
        assertThat(guardada.isTls()).isFalse();
        mvc.perform(get(BASE).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(CLAVE))))
                .andExpect(content().string(containsString("Dejala vacía para mantener la clave guardada.")))
                .andExpect(content().string(containsString("Enviar correo de prueba")));

        mvc.perform(postConCsrf(BASE + "/prueba", BASE).param("destinatario", "destino@test.com"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", containsString("Correo de prueba enviado a destino@test.com")));

        MimeMessage[] recibidos = GREEN_MAIL.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        MimeMessage correo = recibidos[0];
        assertThat(correo.getSubject()).isEqualTo("Correo de prueba de Zero");
        assertThat(correo.getFrom()[0].toString()).contains(CUENTA).contains("Zero Indumentaria Deportiva S.A.");
        assertThat(correo.getAllRecipients()[0].toString()).isEqualTo("destino@test.com");

        String html = (String) buscarParte(correo, "text/html").getContent();
        assertThat(html).contains("¡La configuración funciona!")
                .contains("destino@test.com")
                .contains("cid:logo")
                .contains("Zero Indumentaria Deportiva S.A.")
                .contains("Av. San Martín 1250, Ciudad de Mendoza (5500), Mendoza")
                .contains("contacto@zero.com.ar")
                .contains("background-color: #0b0b0b");
        Part logo = buscarParte(correo, "image/png");
        assertThat(logo).isNotNull();
        assertThat(logo.getHeader("Content-ID")[0]).isEqualTo("<logo>");
    }

    @Test
    void siElServidorNoRespondeLaPantallaMuestraElError() throws Exception {
        guardarConfiguracion(3999);
        mvc.perform(postConCsrf(BASE + "/prueba", BASE).param("destinatario", "destino@test.com"))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("error", startsWith("No se pudo enviar el correo de prueba: ")));
        assertThat(GREEN_MAIL.getReceivedMessages()).isEmpty();
    }

    @Test
    void unaClaveIncorrectaMuestraElError() throws Exception {
        guardarConfiguracion(ServerSetupTest.SMTP.getPort());
        String id = configuracionService.buscarConfiguracionCorreoEmpresa().getId();
        String idSede = configuracionService.buscarConfiguracionCorreoEmpresa().getEmpresa().getId();
        configuracionService.modificarConfiguracionCorreoEmpresa(id, CUENTA, "otra-clave", "3025", "127.0.0.1", false,
                idSede);
        assertThatThrownBy(() -> emailService.enviarPrueba("destino@test.com"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageStartingWith("No se pudo enviar el correo de prueba: ");
    }

    @Test
    void validaLosDatosEnElServidor() throws Exception {
        mvc.perform(postConCsrf(BASE, BASE).param("smtp", "smtp.gmail.com").param("puerto", "99999")
                .param("correo", CUENTA).param("clave", CLAVE))
                .andExpect(flash().attribute("error", "El puerto tiene que ser un número entre 1 y 65535."))
                .andExpect(flash().attribute("puerto", "99999"));
    }

    @Test
    void soloElJefeAccedeAlCorreo() throws Exception {
        MockHttpSession administrativo = login("admin@zero.com.ar", "Admin123!");
        mvc.perform(get(BASE).session(administrativo)).andExpect(status().isForbidden());
    }

    private void guardarConfiguracion(int puerto) throws Exception {
        mvc.perform(postConCsrf(BASE, BASE).param("smtp", "127.0.0.1").param("puerto", String.valueOf(puerto))
                .param("correo", CUENTA).param("clave", CLAVE))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("exito", "Configuración de correo guardada correctamente."));
    }

    // Recorre el MIME (multipart/related con el HTML y el logo inline) hasta encontrar el tipo pedido.
    private Part buscarParte(Part parte, String tipo) throws Exception {
        if (parte.isMimeType(tipo)) {
            return parte;
        }
        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part encontrada = buscarParte(multipart.getBodyPart(i), tipo);
                if (encontrada != null) {
                    return encontrada;
                }
            }
        }
        return null;
    }

    // Login real contra el formulario, con las cuentas que crea el seeder.
    private MockHttpSession login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave))
                .andExpect(redirectedUrl("/admin"));
        return sesion;
    }

    // Obtiene el token del formulario renderizado: prueba también la integración Thymeleaf/Security.
    private MockHttpServletRequestBuilder postConCsrf(String destino, String formulario) throws Exception {
        MvcResult pagina = mvc.perform(get(formulario).session(sesionJefe)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesionJefe).param(token.getParameterName(), token.getToken());
    }
}
