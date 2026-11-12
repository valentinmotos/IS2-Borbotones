package com.dulcemayor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.repository.UsuarioRepository;
import com.dulcemayor.service.EmailService;
import com.dulcemayor.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Usa el filtro OAuth real y un proveedor local con código, PKCE y un ID token firmado. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.oauth.google.enabled=true", "app.oauth.google.client-id=cliente-test",
        "app.oauth.google.client-secret=secreto-test", "app.oauth.google.public-url=http://localhost:5174",
        "spring.datasource.url=jdbc:h2:mem:google-oauth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleOAuthIntegrationTest {

    private static final String BASE = GoogleOAuthFlow.BASE;
    private static final String REDIRECCION = "http://localhost:5174" + GoogleOAuthFlow.CALLBACK;
    private static final String PAGINA = "http://localhost:5174/acceso/google";
    private static final String CLIENTE = "cliente-test";
    private static final String CLAVE = "ClaveOAuth123!";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioService usuarios;
    @Autowired private UsuarioRepository usuarioRepository;
    @LocalServerPort private int puerto;
    @MockitoBean private ClientRegistrationRepository clientes;
    @MockitoBean private EmailService correo;

    private HttpServer proveedor;
    private RSAKey claveFirma;
    private volatile String tokenProveedor;
    private volatile String desafioPkce;

    @BeforeAll
    void configurarProveedor() throws IOException, JOSEException {
        claveFirma = new RSAKeyGenerator(2048).keyID("test").generate();
        proveedor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proveedor.createContext("/jwks", respuesta -> responder(respuesta, 200,
                new JWKSet(claveFirma.toPublicJWK()).toString()));
        proveedor.createContext("/token", this::canjearCodigo);
        proveedor.start();
    }

    @BeforeEach
    void registrarCliente() {
        String url = "http://127.0.0.1:" + proveedor.getAddress().getPort();
        when(clientes.findByRegistrationId("google")).thenReturn(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .issuerUri(GoogleOAuthFlow.EMISOR).clientId(CLIENTE).clientSecret("secreto-test")
                .scope("openid", "email").redirectUri(REDIRECCION)
                .authorizationUri(url + "/autorizar").tokenUri(url + "/token").jwkSetUri(url + "/jwks").build());
    }

    @AfterAll
    void cerrarProveedor() {
        if (proveedor != null) {
            proveedor.stop(0);
        }
    }

    @Test
    void registraActivaYEmiteUnaSesionLocalDeUnSoloUso() throws Exception {
        ArgumentCaptor<Map<String, Object>> variablesCorreo = ArgumentCaptor.captor();
        String email = "alta-google@example.com";
        MockHttpSession navegador = identificar("identidad-alta", email, true);
        JsonNode resultado = estado(navegador, null, "REGISTRO");
        mvc.perform(get("/api/publico/auth/proveedores"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.google").value(true));

        mvc.perform(post(BASE + "/registro").session(navegador).contentType(MediaType.APPLICATION_JSON)
                .header("X-Google-Flow", resultado.path("flujo").asText())
                .content(datos(email, "88901001", "1994-05-05"))).andExpect(status().isForbidden());
        enviar(navegador, resultado, "/registro", datos(email, "88901001", LocalDate.now().minusYears(16).toString()), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("fechaNacimiento"));
        enviar(navegador, resultado, "/registro", datos("otro@example.com", "88901001", "1994-05-05"), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("correo"));
        enviar(navegador, resultado, "/registro", datos(email, "88901001", "1994-05-05"), null)
                .andExpect(status().isAccepted());
        estado(navegador, null, "SIN_FLUJO");
        verify(correo).enviar(eq(email), eq("Activá tu cuenta de Dulce Mayor"), eq("correo/activacion-minorista"),
                variablesCorreo.capture());

        navegador = identificar("identidad-alta", email, true);
        resultado = estado(navegador, null, "PENDIENTE");
        enviar(navegador, resultado, "/sesion", "{}", null).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/publico/registro/activar").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo", email, "codigo", variablesCorreo.getValue().get("codigo")))))
                .andExpect(status().isOk());

        resultado = estado(navegador, null, "VINCULADA");
        JsonNode respuesta = cuerpo(enviar(navegador, resultado, "/sesion", "{}", null)
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store")).andReturn());
        String jwt = respuesta.path("token").asText();
        enviar(navegador, resultado, "/sesion", "{}", null).andExpect(status().isBadRequest());
        mvc.perform(get("/api/minorista/compras").header(HttpHeaders.AUTHORIZATION, bearer(jwt)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(jwt)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(jwt)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(jwt)))
                .andExpect(status().isUnauthorized());
        var cuenta = usuarioRepository.findByNombreUsuario(email).orElseThrow();
        cuenta.setEliminado(true);
        usuarioRepository.save(cuenta);
        navegador = identificar("identidad-alta", email, true);
        resultado = estado(navegador, null, "BLOQUEADA");
        enviar(navegador, resultado, "/sesion", "{}", null).andExpect(status().isUnauthorized());
    }

    @Test
    void vinculaConClaveYDesvincularRevocaElJwtSinCambiarElUsuario() throws Exception {
        String email = "vinculo-google@example.com";
        crearMinorista(email, "88901002");
        String jwtLocal = loginLocal(email);
        String id = usuarioRepository.findByNombreUsuario(email).orElseThrow().getId();
        MockHttpSession navegador = identificar("identidad-vinculo", email, true);
        JsonNode resultado = estado(navegador, jwtLocal, "EXISTENTE");

        enviar(navegador, resultado, "/sesion", "{}", null).andExpect(status().isUnauthorized());
        enviar(navegador, resultado, "/vincular", "{\"clave\":\"incorrecta\"}", jwtLocal)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("clave"));
        enviar(navegador, resultado, "/vincular", clave(), null).andExpect(status().isUnauthorized());
        enviar(navegador, resultado, "/vincular", clave(), jwtLocal).andExpect(status().isNoContent());

        navegador = identificar("identidad-vinculo", "correo-cambio@example.com", true);
        resultado = estado(navegador, null, "VINCULADA");
        String jwtGoogle = cuerpo(enviar(navegador, resultado, "/sesion", "{}", null).andExpect(status().isOk())
                .andReturn()).path("token").asText();
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(jwtGoogle)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));

        resultado = estado(navegador, jwtLocal, "SIN_FLUJO");
        assertThat(resultado.path("vinculada").asBoolean()).isTrue();
        String jwtNuevo = cuerpo(enviar(navegador, resultado, "/desvincular", clave(), jwtLocal)
                .andExpect(status().isOk()).andReturn()).path("token").asText();
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(jwtGoogle)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(jwtNuevo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        estado(identificar("identidad-vinculo", email, true), null, "EXISTENTE");
    }

    @Test
    void rechazaCallbackAlteradoYCorreoNoVerificadoYRestringeAlPersonal() throws Exception {
        Inicio inicio = comenzar();
        mvc.perform(get(GoogleOAuthFlow.CALLBACK).session(inicio.navegador()).param("code", "codigo-test")
                .param("state", "incorrecto")).andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, PAGINA + "?error=acceso"));
        estado(inicio.navegador(), null, "SIN_FLUJO");
        estado(identificar("no-verificado", "no-verificado@example.com", false), null, "SIN_FLUJO");

        String email = "empleado-google@example.com";
        usuarios.crearUsuario(email, RolUsuario.ADMINISTRATIVO, CLAVE, false);
        MockHttpSession navegador = identificar("identidad-empleado", email, true);
        String jwt = loginLocal(email);
        JsonNode resultado = estado(navegador, jwt, "EXISTENTE");
        enviar(navegador, resultado, "/vincular", clave(), jwt).andExpect(status().isForbidden());
        enviar(navegador, resultado, "/sesion", "{}", null).andExpect(status().isUnauthorized());
        enviar(navegador, resultado, "/registro", datos(email, "88901003", "1994-05-05"), null)
                .andExpect(status().isConflict());
    }

    @Test
    void inicioRealUsaCookieHttpOnlySameSiteLax() throws Exception {
        try (HttpClient cliente = HttpClient.newHttpClient()) {
            var uri = URI.create("http://localhost:" + puerto + GoogleOAuthFlow.INICIAR + "/google");
            var respuesta = cliente.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.discarding());
            assertThat(respuesta.statusCode()).isEqualTo(302);
            assertThat(respuesta.headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie ->
                    assertThat(cookie).startsWith("DULCEMAYOR_OAUTH=").contains("HttpOnly", "SameSite=Lax"));
        }
    }

    private void crearMinorista(String email, String dni) throws Exception {
        ArgumentCaptor<Map<String, Object>> variablesCorreo = ArgumentCaptor.captor();
        mvc.perform(post("/api/publico/registro").contentType(MediaType.APPLICATION_JSON)
                .content(datos(email, dni, "1994-05-05"))).andExpect(status().isAccepted());
        verify(correo).enviar(eq(email), eq("Activá tu cuenta de Dulce Mayor"), eq("correo/activacion-minorista"),
                variablesCorreo.capture());
        mvc.perform(post("/api/publico/registro/activar").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correo", email, "codigo", variablesCorreo.getValue().get("codigo")))))
                .andExpect(status().isOk());
    }

    private Inicio comenzar() throws Exception {
        MvcResult respuesta = mvc.perform(get(GoogleOAuthFlow.INICIAR + "/google")).andExpect(status().isFound())
                .andReturn();
        Map<String, String> query = parametros(URI.create(respuesta.getResponse().getRedirectedUrl()).getRawQuery());
        assertThat(query).containsEntry("code_challenge_method", "S256").containsEntry("redirect_uri", REDIRECCION);
        assertThat(query.get("state")).isNotBlank();
        assertThat(query.get("nonce")).isNotBlank();
        return new Inicio((MockHttpSession) respuesta.getRequest().getSession(false), query);
    }

    private MockHttpSession identificar(String sujeto, String email, boolean verificado) throws Exception {
        Inicio inicio = comenzar();
        desafioPkce = inicio.parametros().get("code_challenge");
        Instant ahora = Instant.now();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(),
                new JWTClaimsSet.Builder().issuer(GoogleOAuthFlow.EMISOR).subject(sujeto).audience(CLIENTE)
                        .issueTime(Date.from(ahora)).expirationTime(Date.from(ahora.plusSeconds(60)))
                        .claim("nonce", inicio.parametros().get("nonce")).claim("email", email)
                        .claim("email_verified", verificado).build());
        jwt.sign(new RSASSASigner(claveFirma));
        tokenProveedor = jwt.serialize();
        mvc.perform(get(GoogleOAuthFlow.CALLBACK).session(inicio.navegador()).param("code", "codigo-test")
                .param("state", inicio.parametros().get("state"))).andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, verificado ? PAGINA : PAGINA + "?error=correo"));
        return inicio.navegador();
    }

    private void canjearCodigo(HttpExchange respuesta) throws IOException {
        Map<String, String> formulario = parametros(new String(respuesta.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(formulario.getOrDefault("code_verifier", "").getBytes(StandardCharsets.UTF_8));
            String desafio = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            if (!desafio.equals(desafioPkce) || !"codigo-test".equals(formulario.get("code"))
                    || !REDIRECCION.equals(formulario.get("redirect_uri"))) {
                responder(respuesta, 400, "{\"error\":\"invalid_grant\"}");
                return;
            }
            responder(respuesta, 200, json.writeValueAsString(Map.of("access_token", "token-externo",
                    "token_type", "Bearer", "expires_in", 60, "id_token", tokenProveedor)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void responder(HttpExchange respuesta, int estado, String texto) throws IOException {
        byte[] cuerpo = texto.getBytes(StandardCharsets.UTF_8);
        respuesta.getResponseHeaders().set("Content-Type", "application/json");
        respuesta.sendResponseHeaders(estado, cuerpo.length);
        try (var salida = respuesta.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private static Map<String, String> parametros(String query) {
        Map<String, String> valores = new HashMap<>();
        for (String parte : query.split("&")) {
            String[] par = parte.split("=", 2);
            valores.put(par[0], URLDecoder.decode(par[1], StandardCharsets.UTF_8));
        }
        return valores;
    }

    private JsonNode estado(MockHttpSession navegador, String jwt, String esperado) throws Exception {
        MockHttpServletRequestBuilder request = get(BASE + "/estado").session(navegador);
        if (jwt != null) request.header(HttpHeaders.AUTHORIZATION, bearer(jwt));
        return cuerpo(mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(esperado))
                .andReturn());
    }

    private ResultActions enviar(MockHttpSession navegador, JsonNode estado, String path, String datos, String jwt)
            throws Exception {
        MockHttpServletRequestBuilder request = post(BASE + path).session(navegador)
                .header("X-CSRF-TOKEN", estado.path("csrf").asText())
                .header("X-Google-Flow", estado.path("flujo").asText())
                .contentType(MediaType.APPLICATION_JSON).content(datos);
        if (jwt != null) request.header(HttpHeaders.AUTHORIZATION, bearer(jwt));
        return mvc.perform(request);
    }

    private String loginLocal(String email) throws Exception {
        return cuerpo(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombreUsuario", email, "clave", CLAVE))))
                .andExpect(status().isOk()).andReturn()).path("token").asText();
    }

    private JsonNode cuerpo(MvcResult respuesta) throws IOException {
        return json.readTree(respuesta.getResponse().getContentAsByteArray());
    }

    private String datos(String email, String dni, String fecha) throws IOException {
        return json.writeValueAsString(Map.of("nombre", "Ana", "apellido", "Prueba", "fechaNacimiento", fecha,
                "tipoDocumento", "DNI", "numeroDocumento", dni, "correo", email, "clave", CLAVE));
    }

    private String clave() throws IOException {
        return json.writeValueAsString(Map.of("clave", CLAVE));
    }

    private static String bearer(String jwt) {
        return "Bearer " + jwt;
    }

    private record Inicio(MockHttpSession navegador, Map<String, String> parametros) {
    }
}
