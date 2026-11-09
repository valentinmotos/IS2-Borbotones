package com.dulcemayor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * E1-01 · criterio de aceptación de punta a punta (H2, JWT y BCrypt reales):
 * un minorista recibe 403 en endpoints internos, el primer ingreso con "dulcemayor" obliga a
 * cambiar la clave y la base no guarda claves en texto plano.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadIntegrationTest {

    private static final String CLAVE = "Clave1234";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /** Correo único por test: la base H2 se comparte entre las clases de test. */
    private static String correo(String prefijo) {
        return prefijo + "-" + UUID.randomUUID() + "@dulcemayor.com.ar";
    }

    private ResultActions login(String correo, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Credenciales(correo, clave))));
    }

    private String token(String correo, String clave) throws Exception {
        String cuerpo = login(correo, clave).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("token").asText();
    }

    private String tokenDeUsuarioNuevo(RolUsuario rol) throws Exception {
        String correo = correo(rol.name().toLowerCase());
        usuarioService.crearUsuario(correo, rol, CLAVE, false);
        return token(correo, CLAVE);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credenciales(String nombreUsuario, String clave) {
    }

    // ---- Criterio: el minorista recibe 403 en endpoints internos ----

    @Test
    void minoristaRecibe403EnEndpointsInternos() throws Exception {
        String token = tokenDeUsuarioNuevo(RolUsuario.MINORISTA);

        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tenés permisos para realizar esta acción."));
        // Cualquier endpoint interno, aunque todavía no exista, queda cerrado para el minorista
        mvc.perform(get("/api/pedidos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        // Su propia sesión sí la puede consultar
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("MINORISTA"));
    }

    @Test
    void reglasPorRolEnCategorias() throws Exception {
        for (RolUsuario rol : new RolUsuario[] { RolUsuario.JEFE, RolUsuario.ADMINISTRATIVO }) {
            mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(tokenDeUsuarioNuevo(rol))))
                    .andExpect(status().isOk());
        }
        for (RolUsuario rol : new RolUsuario[] { RolUsuario.PREVENTOR, RolUsuario.DISTRIBUIDOR }) {
            mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(tokenDeUsuarioNuevo(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void sinTokenOConTokenInvalidoRecibe401() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Tenés que iniciar sesión para acceder a este recurso."));
        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer("no.es.un.token")))
                .andExpect(status().isUnauthorized());
    }

    // ---- Criterio: el primer ingreso con "dulcemayor" obliga a cambiar la clave ----

    @Test
    void primerIngresoConClavePorDefectoObligaACambiarla() throws Exception {
        String correo = correo("administrativo");
        usuarioService.crearUsuario(correo, RolUsuario.ADMINISTRATIVO);

        String cuerpo = login(correo, "dulcemayor")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.debeCambiarClave").value(true))
                .andReturn().getResponse().getContentAsString();
        String temporal = objectMapper.readTree(cuerpo).get("token").asText();

        // Con la clave temporal todo está bloqueado salvo /api/auth
        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(temporal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Tenés que cambiar tu clave antes de continuar."));
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(temporal)))
                .andExpect(jsonPath("$.debeCambiarClave").value(true));

        // No puede "cambiarla" por la misma
        mvc.perform(put("/api/auth/clave").header(HttpHeaders.AUTHORIZATION, bearer(temporal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"dulcemayor\",\"nuevaClave\":\"dulcemayor\",\"confirmarClave\":\"dulcemayor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nuevaClave"));

        String respuesta = mvc.perform(put("/api/auth/clave").header(HttpHeaders.AUTHORIZATION, bearer(temporal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"dulcemayor\",\"nuevaClave\":\"MiClave2026\",\"confirmarClave\":\"MiClave2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarClave").value(false))
                .andReturn().getResponse().getContentAsString();
        String definitivo = objectMapper.readTree(respuesta).get("token").asText();

        // El token temporal ya no sirve; el nuevo sí entra
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(temporal)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(definitivo)))
                .andExpect(status().isOk());
        // Y "dulcemayor" dejó de ser su clave
        login(correo, "dulcemayor").andExpect(status().isUnauthorized());
        login(correo, "MiClave2026").andExpect(jsonPath("$.debeCambiarClave").value(false));
    }

    // ---- Criterio: la base no guarda claves en texto plano ----

    @Test
    void laBaseGuardaSoloElHashBCrypt() {
        String correo = correo("preventor");
        usuarioService.crearUsuario(correo, RolUsuario.PREVENTOR);

        Usuario guardado = usuarioRepository.findByNombreUsuario(correo).orElseThrow();
        assertThat(guardado.getClave()).isNotEqualTo("dulcemayor").startsWith("$2a$").hasSize(60);
    }

    // ---- Login y cierre de sesión ----

    @Test
    void loginIncorrectoDevuelve401ConMensaje() throws Exception {
        String correo = correo("jefe");
        usuarioService.crearUsuario(correo, RolUsuario.JEFE, CLAVE, false);

        login(correo, "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o clave incorrectos."));
        login("nadie@dulcemayor.com.ar", CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o clave incorrectos."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"nombreUsuario\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").exists());
    }

    @Test
    void logoutInvalidaElToken() throws Exception {
        String token = tokenDeUsuarioNuevo(RolUsuario.ADMINISTRATIVO);

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDadoDeBajaPierdeElAcceso() throws Exception {
        String correo = correo("distribuidor");
        Usuario usuario = usuarioService.crearUsuario(correo, RolUsuario.DISTRIBUIDOR, CLAVE, false);
        String token = token(correo, CLAVE);

        usuario.setEliminado(true);
        usuarioRepository.save(usuario);

        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        login(correo, CLAVE).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPermiteLosClientesConfigurados() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://sitio-ajeno.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
