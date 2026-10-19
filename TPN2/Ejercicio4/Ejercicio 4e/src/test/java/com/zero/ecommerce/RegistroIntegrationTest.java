package com.zero.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ConfiguracionCorreoEmpresaRepository;
import com.zero.ecommerce.repositories.UsuarioRepository;
import com.zero.ecommerce.services.ConfiguracionCorreoEmpresaService;
import com.zero.ecommerce.services.EmpresaService;
import com.zero.ecommerce.services.UsuarioService;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * Registro y activación de cuenta (E2-05), con el correo real contra GreenMail. No es @Transactional
 * a propósito, igual que EmailAsyncIntegrationTest: el correo sale en otro hilo y tiene que leer la
 * configuración ya guardada. Por eso borra lo que crea al terminar.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
class RegistroIntegrationTest {

    private static final String CORREO = "nuevo.cliente@test.com";
    private static final String CLAVE = "Zapatilla42";

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser(CorreoIntegrationTest.CUENTA, CorreoIntegrationTest.CLAVE));

    private final MockMvc mvc;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionCorreoEmpresaService configuracionService;
    private final ConfiguracionCorreoEmpresaRepository configuracionRepository;
    private final EmpresaService empresaService;
    private final PasswordEncoder passwordEncoder;

    RegistroIntegrationTest(@Autowired MockMvc mvc, @Autowired UsuarioService usuarioService,
            @Autowired UsuarioRepository usuarioRepository,
            @Autowired ConfiguracionCorreoEmpresaService configuracionService,
            @Autowired ConfiguracionCorreoEmpresaRepository configuracionRepository,
            @Autowired EmpresaService empresaService, @Autowired PasswordEncoder passwordEncoder) {
        this.mvc = mvc;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.configuracionService = configuracionService;
        this.configuracionRepository = configuracionRepository;
        this.empresaService = empresaService;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void configurarCorreo() throws Exception {
        configuracionService.crearConfiguracionCorreoEmpresa(CorreoIntegrationTest.CUENTA, CorreoIntegrationTest.CLAVE,
                String.valueOf(ServerSetupTest.SMTP.getPort()), "127.0.0.1", false,
                empresaService.buscarSedeCentral().getId());
    }

    @AfterEach
    void borrarDatos() {
        configuracionRepository.deleteAll();
        for (String correo : List.of(CORREO, "otro.cliente@test.com")) {
            usuarioRepository.findByNombreUsuarioIgnoreCase(correo).ifPresent(usuarioRepository::delete);
        }
    }

    @Test
    void seRegistraRecibeElCodigoYSoloPuedeIngresarDespuesDeActivar() throws Exception {
        String activar = "/registro/activar?correo=nuevo.cliente%40test.com";
        mvc.perform(postConCsrf("/registro").param("correo", " Nuevo.Cliente@test.com ")
                .param("clave", CLAVE).param("confirmacion", CLAVE))
                .andExpect(redirectedUrl(activar))
                .andExpect(flash().attribute("exito", containsString("Te enviamos un código de activación")));

        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(CORREO).orElseThrow();
        assertThat(usuario.getNombreUsuario()).isEqualTo(CORREO);
        assertThat(usuario.getRol()).isEqualTo(RolUsuario.CLIENTE);
        assertThat(usuario.getCodigoActivacion()).matches("\\d{6}");
        assertThat(usuario.getClave()).isNotEqualTo(CLAVE);
        assertThat(passwordEncoder.matches(CLAVE, usuario.getClave())).isTrue();

        // El correo trae el código y el link a la página de activación con el correo cargado.
        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage correo = GREEN_MAIL.getReceivedMessages()[0];
        assertThat(correo.getSubject()).isEqualTo("Activá tu cuenta de Zero");
        assertThat(correo.getAllRecipients()[0].toString()).isEqualTo(CORREO);
        String cuerpo = html(correo);
        assertThat(cuerpo).contains(usuario.getCodigoActivacion()).contains("/registro/activar?correo=nuevo.cliente%40test.com");

        mvc.perform(get("/registro/activar").param("correo", CORREO)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + CORREO + "\"")))
                .andExpect(content().string(containsString("Reenviar código")));

        // Antes de activar no puede ingresar.
        mvc.perform(login(CORREO, CLAVE)).andExpect(redirectedUrl("/login?error=activacion"));

        mvc.perform(postConCsrf("/registro/activar").param("correo", CORREO).param("codigo", "000000x"))
                .andExpect(redirectedUrl(activar))
                .andExpect(flash().attribute("error", "El código de activación no es correcto."));
        mvc.perform(postConCsrf("/registro/activar").param("correo", CORREO)
                .param("codigo", usuario.getCodigoActivacion()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("exito", "¡Tu cuenta quedó activada! Ya podés ingresar."));
        assertThat(usuarioRepository.findByNombreUsuarioIgnoreCase(CORREO).orElseThrow().getCodigoActivacion())
                .isNull();

        // Después sí, y como es cliente va a la home.
        mvc.perform(login(CORREO, CLAVE)).andExpect(redirectedUrl("/"));
        assertThatThrownBy(() -> usuarioService.activarCuenta(CORREO, "123456"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La cuenta ya está activada. Ya podés ingresar.");
    }

    @Test
    void reenviarGeneraUnCodigoNuevoYElAnteriorDejaDeServir() throws Exception {
        String anterior = usuarioService.registrarCliente(CORREO, CLAVE, CLAVE).getCodigoActivacion();

        mvc.perform(postConCsrf("/registro/reenviar").param("correo", CORREO))
                .andExpect(redirectedUrl("/registro/activar?correo=nuevo.cliente%40test.com"))
                .andExpect(flash().attribute("exito", containsString("Te enviamos un código nuevo")));
        assertThat(GREEN_MAIL.waitForIncomingEmail(10_000, 1)).isTrue();
        String nuevo = usuarioRepository.findByNombreUsuarioIgnoreCase(CORREO).orElseThrow().getCodigoActivacion();
        assertThat(html(GREEN_MAIL.getReceivedMessages()[0])).contains(nuevo);

        if (!nuevo.equals(anterior)) {
            assertThatThrownBy(() -> usuarioService.activarCuenta(CORREO, anterior))
                    .hasMessage("El código de activación no es correcto.");
        }
        usuarioService.activarCuenta(CORREO, nuevo);
        assertThatThrownBy(() -> usuarioService.reenviarCodigoActivacion(CORREO))
                .hasMessage("La cuenta ya está activada. Ya podés ingresar.");
        assertThatThrownBy(() -> usuarioService.reenviarCodigoActivacion("nadie@test.com"))
                .hasMessage("No hay una cuenta registrada con el correo nadie@test.com.");
    }

    @Test
    void validaElCorreoLaClaveYLaConfirmacion() throws Exception {
        assertThatThrownBy(() -> usuarioService.registrarCliente("", CLAVE, CLAVE))
                .hasMessage("El correo electrónico es obligatorio.");
        assertThatThrownBy(() -> usuarioService.registrarCliente("sin-arroba.com", CLAVE, CLAVE))
                .hasMessage("El correo electrónico no tiene un formato válido.");
        assertThatThrownBy(() -> usuarioService.registrarCliente(CORREO, "corta", "corta"))
                .hasMessage("La clave tiene que tener al menos 8 caracteres.");
        assertThatThrownBy(() -> usuarioService.registrarCliente(CORREO, CLAVE, "OtraClave99"))
                .hasMessage("La confirmación no coincide con la clave.");
        assertThatThrownBy(() -> usuarioService.registrarCliente("JEFE@zero.com.ar", CLAVE, CLAVE))
                .hasMessage("Ya hay una cuenta registrada con ese correo.");

        usuarioService.registrarCliente(CORREO, CLAVE, CLAVE);
        assertThatThrownBy(() -> usuarioService.registrarCliente(CORREO, CLAVE, CLAVE))
                .hasMessageStartingWith("Ya hay una cuenta registrada con ese correo que todavía no se activó.");

        // El formulario vuelve con el error y el correo, pero nunca con la clave.
        MvcResult error = mvc.perform(postConCsrf("/registro").param("correo", "otro.cliente@test.com")
                .param("clave", CLAVE).param("confirmacion", "OtraClave99"))
                .andExpect(redirectedUrl("/registro"))
                .andExpect(flash().attribute("error", "La confirmación no coincide con la clave.")).andReturn();
        mvc.perform(get("/registro").flashAttrs(error.getFlashMap()))
                .andExpect(content().string(containsString("value=\"otro.cliente@test.com\"")))
                .andExpect(content().string(containsString("La confirmación no coincide con la clave.")))
                .andExpect(content().string(not(containsString(CLAVE))));
        assertThat(usuarioRepository.findByNombreUsuarioIgnoreCase("otro.cliente@test.com")).isEmpty();
    }

    @Test
    void elLoginYElHeaderLlevanAlRegistro() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/registro\"")));
        mvc.perform(get("/login").param("error", "activacion"))
                .andExpect(content().string(containsString("href=\"/registro/activar\"")));
        mvc.perform(get("/registro")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Crear cuenta en Zero")));
    }

    // El HTML del correo ya decodificado (el cuerpo crudo viene en quoted-printable).
    private String html(Part parte) throws Exception {
        if (parte.isMimeType("text/html")) {
            return (String) parte.getContent();
        }
        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String encontrado = html(multipart.getBodyPart(i));
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private MockHttpServletRequestBuilder postConCsrf(String destino) throws Exception {
        MvcResult pagina = mvc.perform(get("/registro")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post(destino).session(sesion).param(token.getParameterName(), token.getToken());
    }

    private MockHttpServletRequestBuilder login(String correo, String clave) throws Exception {
        MvcResult pagina = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession();
        CsrfToken token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        return post("/login").session(sesion).param(token.getParameterName(), token.getToken())
                .param("username", correo).param("password", clave);
    }
}
