package com.borbotones.cliente;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="debug=false")
@AutoConfigureMockMvc
@Import(ClienteOAuth2Tests.HttpSimulado.class)
class ClienteOAuth2Tests {
    static MockRestServiceServer upstream;
    @TestConfiguration
    static class HttpSimulado {
        @Bean @Primary RestClient.Builder builder() {
            var builder=RestClient.builder(); upstream=MockRestServiceServer.bindTo(builder).build(); return builder;
        }
    }
    @Autowired MockMvc mvc;
    @Autowired ClientRegistrationRepository registros;
    @MockitoBean OAuth2AuthorizedClientManager manager;
    @BeforeEach void iniciar() {
        upstream.reset();
        var token=new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,"token-solo-del-servidor",
                Instant.now(),Instant.now().plusSeconds(300),Set.of("api.read","api.write"));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(registros.findByRegistrationId("local"),"usuario",token));
    }
    @Test void inicioPublicoPeroSesionYApiPrivados() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/registro")).andExpect(status().isFound()).andExpect(redirectedUrl("http://127.0.0.1:9000/registro"));
    }
    @Test void loginRedirigeConStateNonceYPkce() throws Exception {
        var result=mvc.perform(get("/oauth2/authorization/local")).andExpect(status().isFound()).andReturn();
        String location=result.getResponse().getHeader("Location");
        assertThat(location).startsWith("http://127.0.0.1:9000/oauth2/authorize?")
                .contains("response_type=code","state=","nonce=","code_challenge=","code_challenge_method=S256");
    }
    @Test void operacionesConSesionExigenCsrf() throws Exception {
        mvc.perform(post("/api/mascotas").with(oidcLogin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(oidcLogin())).andExpect(status().isForbidden());
        verifyNoInteractions(manager);
    }
    @Test void sesionDevuelveCsrfPeroNoTokensOAuth() throws Exception {
        mvc.perform(get("/sesion").with(oidcLogin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }
    @Test void proxyAgregaAccessTokenSinReenviarCookies() throws Exception {
        upstream.expect(requestTo("http://127.0.0.1:8000/api/mascotas"))
                .andExpect(header("Authorization","Bearer token-solo-del-servidor"))
                .andExpect(headerDoesNotExist("Cookie")).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/mascotas").with(oidcLogin())).andExpect(status().isOk()).andExpect(content().json("[]"));
        upstream.verify();
    }
    @Test void proxyConservaJsonYErroresDePermisoDelServidor() throws Exception {
        upstream.expect(requestTo("http://127.0.0.1:8000/api/mascotas/ajena"))
                .andExpect(method(HttpMethod.PUT)).andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json("{\"nombre\":\"Luna\"}"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("{\"mensaje\":\"Mascota ajena\"}"));
        mvc.perform(put("/api/mascotas/ajena").with(oidcLogin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Luna\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").value("Mascota ajena"));
        upstream.verify();
    }
    @Test void proxyConservaFotosMultipart() throws Exception {
        upstream.expect(requestTo("http://127.0.0.1:8000/api/mascotas/propia/foto"))
                .andExpect(method(HttpMethod.PUT)).andExpect(header("Content-Type",containsString("multipart/form-data")))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        var foto=new MockMultipartFile("archivo","foto.png","image/png",new byte[]{1,2,3});
        mvc.perform(multipart(HttpMethod.PUT,"/api/mascotas/propia/foto").file(foto).with(oidcLogin()).with(csrf()))
                .andExpect(status().isNoContent());
        upstream.verify();
    }
    @Test void falloAlRenovarNoExponeDetallesInternos() throws Exception {
        when(manager.authorize(any())).thenThrow(new OAuth2AuthorizationException(new OAuth2Error("invalid_grant")));
        mvc.perform(get("/api/mascotas").with(oidcLogin())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("La sesion vencio. Vuelve a ingresar"));
    }
}
